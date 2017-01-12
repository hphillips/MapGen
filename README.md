# MapMaker
A project I'm working on for procedural generation of a node-edge based game map. Major problems are determining which nodes should be connected and determining different regions of the map.

AreaMap is the main file. When you run it you'll have control over how many points are generated and the tolerance level for creating a new path. (A new path will be created if there is no path that connects the two points of if the existing path is sufficiently longer than the direct path, based on the tolerance level (default 40%). 

Clustering is currently done, but each cluster needs to be expanded to beyond the hull created by cluster, which is next on my todo list.
