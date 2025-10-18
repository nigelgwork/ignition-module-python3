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
import os
from typing import Any, Dict

# Resource limits (configured via environment variables)
# Memory limit: 512MB default (can be overridden with PYTHON3_MAX_MEMORY_MB)
# CPU time limit: 60 seconds default (can be overridden with PYTHON3_MAX_CPU_SECONDS)
MAX_MEMORY_MB = int(os.environ.get('PYTHON3_MAX_MEMORY_MB', '512'))
MAX_CPU_SECONDS = int(os.environ.get('PYTHON3_MAX_CPU_SECONDS', '60'))

# Apply resource limits (Unix/Linux only)
try:
    import resource

    # Set memory limit (virtual memory)
    max_memory_bytes = MAX_MEMORY_MB * 1024 * 1024
    resource.setrlimit(resource.RLIMIT_AS, (max_memory_bytes, max_memory_bytes))
    print(f"Resource limit applied: Max memory = {MAX_MEMORY_MB}MB", file=sys.stderr)

    # Set CPU time limit (prevents runaway CPU-intensive scripts)
    resource.setrlimit(resource.RLIMIT_CPU, (MAX_CPU_SECONDS, MAX_CPU_SECONDS))
    print(f"Resource limit applied: Max CPU time = {MAX_CPU_SECONDS}s", file=sys.stderr)

except ImportError:
    # Windows doesn't have resource module - log warning
    print("WARNING: resource module not available (Windows?). Resource limits not applied.", file=sys.stderr)
except Exception as e:
    # Non-fatal: log error but continue
    print(f"WARNING: Failed to apply resource limits: {e}", file=sys.stderr)


class SecurityException(Exception):
    """Raised when code violates security policy"""
    pass


class PythonBridge:
    """Handles communication between Java and Python 3"""

    def __init__(self):
        self.globals_dict = {}
        self.version = sys.version

        # Security: Module whitelist (safe modules allowed in RESTRICTED mode)
        self.safe_modules = {
            'math', 'json', 'datetime', 'itertools', 'collections',
            'decimal', 'random', 're', 'statistics', 'time', 'calendar',
            'uuid', 'hashlib', 'base64', 'string', 'textwrap',
            'difflib', 'enum', 'functools', 'operator', 'copy',
            'ast'  # For syntax checking
        }

        # Security: Admin modules (allowed ONLY in ADMIN mode)
        self.admin_modules = {
            'os', 'subprocess', 'sys', 'socket', 'urllib', 'requests',
            'http', 'ftplib', 'smtplib', 'pathlib', 'shutil', 'glob',
            'tempfile', 'sqlite3', 'csv', 'xml', 'pandas', 'numpy',
            'pickle', 'shelve', 'dbm'
        }

        # Security: Always blocked modules (dangerous even for admins)
        self.always_blocked_modules = {
            'telnetlib', 'paramiko', 'pty', 'tty', 'termios',
            'fcntl', 'pipes', 'posix', 'pwd', 'grp', 'crypt',
            'ctypes', 'multiprocessing', 'threading', 'asyncio',
            'concurrent', '__builtin__', 'builtins'
        }

        # Security: Blocked functions (dangerous built-ins - blocked in RESTRICTED mode)
        self.blocked_functions = {
            '__import__', 'eval', 'exec', 'compile', 'open',
            'input', 'raw_input', 'file', 'execfile', 'reload',
            'vars', 'locals', 'globals', 'dir', 'getattr', 'setattr',
            'delattr', 'hasattr'
        }

    def _validate_code_security(self, code: str, security_mode: str = "RESTRICTED") -> None:
        """Validate code for security violations (raises exception if unsafe)

        Security modes:
        - RESTRICTED: Only safe_modules allowed (default, for regular users)
        - ADMIN: safe_modules + admin_modules allowed (for Ignition Administrators)
        """
        code_upper = code.upper()

        # Always check for always-blocked modules
        for module in self.always_blocked_modules:
            if f'IMPORT {module.upper()}' in code_upper or f'FROM {module.upper()} IMPORT' in code_upper:
                raise SecurityException(
                    f"Security violation: Module '{module}' is always blocked for security reasons"
                )

        # In RESTRICTED mode, block admin modules
        if security_mode == "RESTRICTED":
            blocked_modules = self.admin_modules

            for module in blocked_modules:
                # Check "import module" pattern
                if f'IMPORT {module.upper()}' in code_upper:
                    raise SecurityException(
                        f"Security violation: Import of '{module}' requires Administrator role. "
                        f"Allowed modules: {', '.join(sorted(self.safe_modules))}"
                    )
                # Check "from module import" pattern
                if f'FROM {module.upper()} IMPORT' in code_upper:
                    raise SecurityException(
                        f"Security violation: Import from '{module}' requires Administrator role. "
                        f"Allowed modules: {', '.join(sorted(self.safe_modules))}"
                    )

            # Check for dangerous function calls (only in RESTRICTED mode)
            for func in self.blocked_functions:
                if func in code or func.upper() in code_upper:
                    raise SecurityException(
                        f"Security violation: Function '{func}' requires Administrator role"
                    )

            # Check for dangerous eval/exec patterns (only in RESTRICTED mode)
            dangerous_patterns = [
                ('EVAL(', 'eval()'), ('EXEC(', 'exec()'), ('__IMPORT__', '__import__'),
                ('COMPILE(', 'compile()'), ('OPEN(', 'open()'),
                ('SUBPROCESS.', 'subprocess'), ('OS.', 'os module'),
                ('SYS.', 'sys module'), ('SOCKET.', 'socket module')
            ]

            for pattern, description in dangerous_patterns:
                if pattern in code_upper:
                    raise SecurityException(
                        f"Security violation: Use of {description} requires Administrator role"
                    )

        # In ADMIN mode, allow admin modules but still log usage
        elif security_mode == "ADMIN":
            # Just log admin module usage for audit
            for module in self.admin_modules:
                if f'IMPORT {module.upper()}' in code_upper or f'FROM {module.upper()} IMPORT' in code_upper:
                    print(f"ADMIN MODE: Using privileged module '{module}'", file=sys.stderr)

    def _safe_import(self, name: str, security_mode: str = "RESTRICTED", *args, **kwargs):
        """Restricted import function for safe module loading

        Security modes:
        - RESTRICTED: Only safe_modules allowed
        - ADMIN: safe_modules + admin_modules allowed
        """
        # Check if module is always blocked
        if name in self.always_blocked_modules or name.split('.')[0] in self.always_blocked_modules:
            raise ImportError(
                f"Module '{name}' is always blocked for security reasons"
            )

        # In RESTRICTED mode, only allow safe modules
        if security_mode == "RESTRICTED":
            if name not in self.safe_modules and name.split('.')[0] not in self.safe_modules:
                raise ImportError(
                    f"Module '{name}' requires Administrator role. "
                    f"Allowed modules: {', '.join(sorted(self.safe_modules))}"
                )

        # In ADMIN mode, allow safe + admin modules
        elif security_mode == "ADMIN":
            allowed = self.safe_modules | self.admin_modules
            if name not in allowed and name.split('.')[0] not in allowed:
                raise ImportError(
                    f"Module '{name}' is not in the approved whitelist. "
                    f"Allowed modules: {', '.join(sorted(allowed))}"
                )

        # Import the module
        return importlib.import_module(name)

    def execute_code(self, code: str, variables: Dict[str, Any] = None, security_mode: str = "RESTRICTED") -> Dict[str, Any]:
        """Execute Python code in restricted environment

        Security modes:
        - RESTRICTED: Only safe_modules allowed (default)
        - ADMIN: safe_modules + admin_modules allowed (for Ignition Administrators)
        """
        try:
            # SECURITY CHECK: Validate code before execution
            self._validate_code_security(code, security_mode)

            # Merge provided variables with globals
            exec_globals = self.globals_dict.copy()
            if variables:
                exec_globals.update(variables)

            # Add safe __import__ override with security mode
            exec_globals['__import__'] = lambda name, *args, **kwargs: self._safe_import(name, security_mode, *args, **kwargs)

            # Remove dangerous builtins (only in RESTRICTED mode)
            if security_mode == "RESTRICTED":
                safe_builtins = {k: v for k, v in __builtins__.items()
                               if k not in self.blocked_functions}
                exec_globals['__builtins__'] = safe_builtins
            else:  # ADMIN mode - allow all builtins
                exec_globals['__builtins__'] = __builtins__

            # Capture stdout during execution
            stdout_capture = io.StringIO()

            with contextlib.redirect_stdout(stdout_capture):
                # Execute code in restricted environment
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

        except SecurityException as e:
            return {
                'success': False,
                'error': f"SECURITY ERROR: {str(e)}",
                'traceback': ''  # Don't expose internal stack trace for security errors
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }

    def evaluate_expression(self, expression: str, variables: Dict[str, Any] = None, security_mode: str = "RESTRICTED") -> Dict[str, Any]:
        """Evaluate a Python expression in restricted environment

        Security modes:
        - RESTRICTED: Only safe_modules allowed (default)
        - ADMIN: safe_modules + admin_modules allowed (for Ignition Administrators)
        """
        try:
            # SECURITY CHECK: Validate expression before evaluation
            self._validate_code_security(expression, security_mode)

            # Merge provided variables with globals
            eval_globals = self.globals_dict.copy()
            if variables:
                eval_globals.update(variables)

            # Add safe __import__ override with security mode
            eval_globals['__import__'] = lambda name, *args, **kwargs: self._safe_import(name, security_mode, *args, **kwargs)

            # Remove dangerous builtins (only in RESTRICTED mode)
            if security_mode == "RESTRICTED":
                safe_builtins = {k: v for k, v in __builtins__.items()
                               if k not in self.blocked_functions}
                eval_globals['__builtins__'] = safe_builtins
            else:  # ADMIN mode - allow all builtins
                eval_globals['__builtins__'] = __builtins__

            # Evaluate expression in restricted environment
            result = eval(expression, eval_globals)

            return {
                'success': True,
                'result': self._serialize(result)
            }

        except SecurityException as e:
            return {
                'success': False,
                'error': f"SECURITY ERROR: {str(e)}",
                'traceback': ''  # Don't expose internal stack trace for security errors
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }

    def call_module(self, module_name: str, function_name: str, args: list = None, kwargs: dict = None, security_mode: str = "RESTRICTED") -> Dict[str, Any]:
        """Import a module and call a function (with security checks)

        Security modes:
        - RESTRICTED: Only safe_modules allowed (default)
        - ADMIN: safe_modules + admin_modules allowed (for Ignition Administrators)
        """
        try:
            # SECURITY CHECK: Validate module is always-blocked
            if module_name in self.always_blocked_modules or module_name.split('.')[0] in self.always_blocked_modules:
                raise SecurityException(
                    f"Module '{module_name}' is always blocked for security reasons"
                )

            # In RESTRICTED mode, only allow safe modules
            if security_mode == "RESTRICTED":
                if module_name not in self.safe_modules and module_name.split('.')[0] not in self.safe_modules:
                    raise SecurityException(
                        f"Module '{module_name}' requires Administrator role. "
                        f"Allowed modules: {', '.join(sorted(self.safe_modules))}"
                    )

            # In ADMIN mode, allow safe + admin modules
            elif security_mode == "ADMIN":
                allowed = self.safe_modules | self.admin_modules
                if module_name not in allowed and module_name.split('.')[0] not in allowed:
                    raise SecurityException(
                        f"Module '{module_name}' is not in the approved whitelist. "
                        f"Allowed modules: {', '.join(sorted(allowed))}"
                    )

            # Import module using safe import with security mode
            module = self._safe_import(module_name, security_mode)

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

        except SecurityException as e:
            return {
                'success': False,
                'error': f"SECURITY ERROR: {str(e)}",
                'traceback': ''  # Don't expose internal stack trace for security errors
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

    def get_completions(self, code: str, line: int, column: int) -> Dict[str, Any]:
        """Get code completions at cursor position using Jedi"""
        try:
            try:
                import jedi
            except ImportError:
                # Jedi not installed, return fallback completions
                return {
                    'success': True,
                    'result': {
                        'completions': [],
                        'message': 'Jedi library not installed. Install with: pip install jedi'
                    }
                }

            # Create Jedi script for analysis
            script = jedi.Script(code, path='<stdin>')

            # Get completions at cursor position (Jedi uses 1-based line numbers)
            completions = script.complete(line, column)

            # Format completion results
            completion_list = []
            for completion in completions[:50]:  # Limit to 50 results
                try:
                    completion_item = {
                        'text': completion.name,
                        'type': completion.type,  # 'function', 'class', 'module', 'keyword', etc.
                        'complete': completion.complete,  # Full completion text
                    }

                    # Add description if available
                    try:
                        if completion.docstring():
                            # Extract first line of docstring for summary
                            doc_lines = completion.docstring().split('\n')
                            summary = doc_lines[0] if doc_lines else ''
                            completion_item['description'] = summary[:100]  # Limit description
                            completion_item['docstring'] = completion.docstring()[:500]  # Full docstring (limited)
                    except Exception:
                        pass

                    # Add function signature if available
                    try:
                        if completion.type in ('function', 'class'):
                            signatures = completion.get_signatures()
                            if signatures:
                                sig = signatures[0]
                                params = []
                                for param in sig.params:
                                    param_str = param.name
                                    if param.infer_default():
                                        try:
                                            default_val = str(param.infer_default()[0].name)
                                            param_str += f'={default_val}'
                                        except Exception:
                                            pass
                                    params.append(param_str)
                                completion_item['signature'] = f"{completion.name}({', '.join(params)})"
                    except Exception:
                        pass

                    completion_list.append(completion_item)

                except Exception:
                    # Skip this completion if there's an error
                    continue

            return {
                'success': True,
                'result': {
                    'completions': completion_list,
                    'count': len(completion_list)
                }
            }

        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }

    def execute_shell(self, command: str, timeout: int = 30) -> Dict[str, Any]:
        """Execute shell command and return stdout, stderr, exit code

        v2.5.0: Added for Shell Command mode in Designer IDE

        Args:
            command: Shell command to execute
            timeout: Command timeout in seconds (default: 30)

        Returns:
            Dict with success, stdout, stderr, exitCode
        """
        try:
            import subprocess

            # Execute shell command
            result = subprocess.run(
                command,
                shell=True,
                capture_output=True,
                text=True,
                timeout=timeout
            )

            return {
                'success': result.returncode == 0,
                'stdout': result.stdout,
                'stderr': result.stderr,
                'exitCode': result.returncode,
                'result': {
                    'stdout': result.stdout,
                    'stderr': result.stderr,
                    'exitCode': result.returncode
                }
            }

        except subprocess.TimeoutExpired:
            return {
                'success': False,
                'error': f'Command timed out after {timeout} seconds',
                'stdout': '',
                'stderr': '',
                'exitCode': -1
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc(),
                'stdout': '',
                'stderr': '',
                'exitCode': -1
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

        # Extract security_mode from request (default to RESTRICTED)
        security_mode = request.get('security_mode', 'RESTRICTED')

        if command == 'execute':
            return self.execute_code(
                request.get('code', ''),
                request.get('variables'),
                security_mode
            )

        elif command == 'evaluate':
            return self.evaluate_expression(
                request.get('expression', ''),
                request.get('variables'),
                security_mode
            )

        elif command == 'call_module':
            return self.call_module(
                request.get('module'),
                request.get('function'),
                request.get('args'),
                request.get('kwargs'),
                security_mode
            )

        elif command == 'version':
            return self.get_version()

        elif command == 'list_modules':
            return self.list_modules()

        elif command == 'clear_globals':
            return self.clear_globals()

        elif command == 'check_syntax':
            return self.check_syntax(request.get('code', ''))

        elif command == 'get_completions':
            return self.get_completions(
                request.get('code', ''),
                request.get('line', 1),
                request.get('column', 0)
            )

        elif command == 'execute_shell':
            return self.execute_shell(
                request.get('command_str', ''),
                request.get('timeout', 30)
            )

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
