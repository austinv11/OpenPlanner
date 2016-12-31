# OpenPlanner
This is an opensource project which aims at providing a way to easily manage day-to-day tasks.

## Major Design Goals
* Provide a server launcher which guarantees 100% uptime (it should manage and update server instances automagically)
* Provide an extensive plugin system which allows for complete seperation of task logic and server networking. These plugins should also
be able to be hotswapped.
* Provide a decentralized plugin hosting repo system
* Allow for high levels of user configuration (for the root user hosting the server)
* Expose a REST and websocket api for custom clients
* Provide an exemplar client implementation
