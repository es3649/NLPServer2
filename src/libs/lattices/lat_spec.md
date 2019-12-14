# Lattices

The `.lat` specification is really just some json. 
There's not too much fancy about it. It's just written specifically to encode word lattices.

The word lattices are denoted by roted, weighted, directed graphs.
A root must be specified, (and can have no text to specifiy multiple roots).
From there, a lattice can be specified, which is a glorified map (adjacency representation of a graph).

Each node is labeled by an ID, which other nodes use to refer to it in adjacency lists. 
Each node must also specify a list of weights corresponding to the node transitions.
If all weights are set to 0, then at runtime they will be corrected to a uniform distribution among the transitions.
Transitions weights need not be normalized to 1. They will be normalized at runtime.

Here is an example lattice:

``` json
{
    "root":"a",
    "lattice":{
        "a":{
            "v":"",
            "to":["b","c"],
            "w":[0,0]
        },
        "b":{
            "v":"hey,",
            "to":["d", "e", "f"],
            "w":[]
        },
        "c":{
            "v":"yeah",
            "to":["d","e","f"],
            "w":[]
        },
        "d":{
            "v":"no problem",
            "to":["g","h"],
            "w":[7,3]
        },
        "e":{
            "v":"you're welcome",
            "to":["g","h"],
            "w":[1,2]
        },
        "f":{
            "v":"my pleasure",
            "to":[],
            "w":[]
        },
        "g":{
            "v":"<f-name>",
            "to":[],
            "w":[]
        },
        "h":{
            "v":"",
            "to":[],
            "w":[]
        }

    }
}
```

The `root` element specifies thge root of the graph.

The `lattice` element specified the graph. 
It's a map of node names to their properties.
Each value in the lattice map requires three arguments, the first, `v`, is the text value of the word at this entry of the lattice. This entry may have formatting directives such as in `g`. The following tages are available:
```xml
<f-name> is replaced with the first name
<f-date> is replaced by the month and day
<f-date-ordinal> is replaced by the day number with it's ordinal modifier
<f-time> is replaced by a time of day
```
It may be worth considering some other directives such as:
``` xml
<f-month> replaced by the month
<f-date-relative> which will be replaced with a relative date term such as "this Sunday" or "next Wednesday"
```


The `to` specifies transition destinations.
This is a list of lattice-node keys, as they appear in the lattice map

The `w` element specifies a list of weights corresponding to nodes in the `to` element. 
The length of this list must match exactly the length of the `to` element or errors will be thrown at runtime.
These weignts need not be floats, as they will be normalized at runtime. If they are left to 0, they will be replaced by a uniform distribution at runtime.