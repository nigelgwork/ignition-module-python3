#!/usr/bin/env python3
"""
Python Bridge Script for Ignition Python 3 Integration
This script runs as a persistent subprocess and handles JSON-RPC style commands.
"""

import sys
import json
import traceback
import importlib
import io
import contextlib
from typing import Any, Dict


class PythonBridge:
    """Handles communication between Java and Python 3"""

    def __init__(self):
        self.globals_dict = {}
        self.version = sys.version

    def execute_code(self, code: str, variables: Dict[str, Any] = None) -> Dict[str, Any]:
        """Execute arbitrary Python code"""
        try:
            # Merge provided variables with globals
            exec_globals = self.globals_dict.copy()
            if variables:
                exec_globals.update(variables)

            # Capture stdout during execution
            stdout_capture = io.StringIO()

            with contextlib.redirect_stdout(stdout_capture):
                # Execute code
                exec_locals = {}
                exec(code, exec_globals, exec_locals)

            # Get captured output
            captured_output = stdout_capture.getvalue()

            # Update globals with new definitions
            self.globals_dict.update(exec_locals)

            # Return the 'result' variable if it exists, otherwise return captured output
            result = exec_locals.get('result', captured_output if captured_output else None)

            return {
                'success': True,
                'result': self._serialize(result),
                'output': captured_output if captured_output else None
            }

        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }

    def evaluate_expression(self, expression: str, variables: Dict[str, Any] = None) -> Dict[str, Any]:
        """Evaluate a Python expression and return result"""
        try:
            # Merge provided variables with globals
            eval_globals = self.globals_dict.copy()
            if variables:
                eval_globals.update(variables)

            # Evaluate expression
            result = eval(expression, eval_globals)

            return {
                'success': True,
                'result': self._serialize(result)
            }

        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }

    def call_module(self, module_name: str, function_name: str, args: list = None, kwargs: dict = None) -> Dict[str, Any]:
        """Import a module and call a function"""
        try:
            # Import module
            module = importlib.import_module(module_name)

            # Get function
            if not hasattr(module, function_name):
                raise AttributeError(f"Module '{module_name}' has no attribute '{function_name}'")

            func = getattr(module, function_name)

            # Call function
            args = args or []
            kwargs = kwargs or {}
            result = func(*args, **kwargs)

            return {
                'success': True,
                'result': self._serialize(result)
            }

        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }

    def get_version(self) -> Dict[str, Any]:
        """Get Python version information"""
        return {
            'success': True,
            'result': {
                'version': sys.version,
                'version_info': {
                    'major': sys.version_info.major,
                    'minor': sys.version_info.minor,
                    'micro': sys.version_info.micro
                },
                'executable': sys.executable,
                'platform': sys.platform
            }
        }

    def list_modules(self) -> Dict[str, Any]:
        """List installed modules"""
        try:
            import pkg_resources
            installed = [pkg.key for pkg in pkg_resources.working_set]
            installed.sort()

            return {
                'success': True,
                'result': installed
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }

    def clear_globals(self) -> Dict[str, Any]:
        """Clear global variables"""
        self.globals_dict.clear()
        return {
            'success': True,
            'result': 'Globals cleared'
        }

    def check_syntax(self, code: str) -> Dict[str, Any]:
        """Check Python code for syntax errors using AST and pyflakes"""
        try:
            import ast
            errors = []

            # First, check for syntax errors using AST
            try:
                ast.parse(code)
            except SyntaxError as e:
                errors.append({
                    'line': e.lineno if e.lineno else 1,
                    'column': e.offset if e.offset else 0,
                    'message': str(e.msg) if e.msg else str(e),
                    'severity': 'error'
                })

            # If no syntax errors, try pyflakes for additional checks
            if not errors:
                try:
                    import pyflakes.api
                    import pyflakes.reporter

                    # Capture pyflakes warnings
                    warning_stream = io.StringIO()
                    error_stream = io.StringIO()

                    # Custom reporter to capture warnings
                    reporter = pyflakes.reporter.Reporter(warning_stream, error_stream)
                    pyflakes.api.check(code, '<string>', reporter=reporter)

                    # Parse pyflakes output
                    warnings = warning_stream.getvalue()
                    if warnings:
                        for line in warnings.strip().split('\n'):
                            if line:
                                # Format: <string>:line:col: message
                                parts = line.split(':', 3)
                                if len(parts) >= 4:
                                    try:
                                        line_num = int(parts[1])
                                        col_num = int(parts[2]) if parts[2].strip().isdigit() else 0
                                        message = parts[3].strip()
                                        errors.append({
                                            'line': line_num,
                                            'column': col_num,
                                            'message': message,
                                            'severity': 'warning'
                                        })
                                    except (ValueError, IndexError):
                                        pass

                except ImportError:
                    # pyflakes not installed, skip additional checks
                    pass
                except Exception:
                    # Don't fail if pyflakes check fails
                    pass

            return {
                'success': True,
                'result': {'errors': errors}
            }

        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }

    def _serialize(self, obj: Any) -> Any:
        """Convert Python objects to JSON-serializable format"""
        if obj is None:
            return None
        elif isinstance(obj, (bool, int, float, str)):
            return obj
        elif isinstance(obj, (list, tuple)):
            return [self._serialize(item) for item in obj]
        elif isinstance(obj, dict):
            return {str(k): self._serialize(v) for k, v in obj.items()}
        elif isinstance(obj, set):
            return list(obj)
        elif isinstance(obj, bytes):
            return obj.decode('utf-8', errors='replace')
        else:
            # For objects, try to convert to string
            return str(obj)

    def process_request(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """Process a request and return response"""
        command = request.get('command')

        if command == 'execute':
            return self.execute_code(
                request.get('code', ''),
                request.get('variables')
            )

        elif command == 'evaluate':
            return self.evaluate_expression(
                request.get('expression', ''),
                request.get('variables')
            )

        elif command == 'call_module':
            return self.call_module(
                request.get('module'),
                request.get('function'),
                request.get('args'),
                request.get('kwargs')
            )

        elif command == 'version':
            return self.get_version()

        elif command == 'list_modules':
            return self.list_modules()

        elif command == 'clear_globals':
            return self.clear_globals()

        elif command == 'check_syntax':
            return self.check_syntax(request.get('code', ''))

        elif command == 'ping':
            return {'success': True, 'result': 'pong'}

        else:
            return {
                'success': False,
                'error': f"Unknown command: {command}"
            }

    def run(self):
        """Main loop: read requests from stdin, write responses to stdout"""
        # Signal ready
        sys.stdout.write(json.dumps({'status': 'ready'}) + '\n')
        sys.stdout.flush()

        while True:
            try:
                # Read request (line-based JSON)
                line = sys.stdin.readline()

                if not line:
                    # EOF - exit gracefully
                    break

                # Parse request
                request = json.loads(line.strip())

                # Check for shutdown command
                if request.get('command') == 'shutdown':
                    sys.stdout.write(json.dumps({'success': True, 'result': 'shutting down'}) + '\n')
                    sys.stdout.flush()
                    break

                # Process request
                response = self.process_request(request)

                # Write response
                sys.stdout.write(json.dumps(response) + '\n')
                sys.stdout.flush()

            except json.JSONDecodeError as e:
                error_response = {
                    'success': False,
                    'error': f"JSON decode error: {str(e)}"
                }
                sys.stdout.write(json.dumps(error_response) + '\n')
                sys.stdout.flush()

            except Exception as e:
                error_response = {
                    'success': False,
                    'error': f"Unexpected error: {str(e)}",
                    'traceback': traceback.format_exc()
                }
                sys.stdout.write(json.dumps(error_response) + '\n')
                sys.stdout.flush()


if __name__ == '__main__':
    bridge = PythonBridge()
    bridge.run()
