What is this?
=============

Vanilla CI is a bare-boned continuous integration server.

"Vanilla" because it's plain, like vanilla ice cream.
The idea is to focus on the core server without worrying about specific features (such as SCM, build tools, etc).
Everything except the core features (basic slave management, remote execution of jobs, and queuing) will be plugins.
This should keep the core code simple and easy to maintain.