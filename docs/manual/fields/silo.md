---
id: silo
title: Tower silo and grain bin
chapter: fields
order: 30
icon: farmings:silo_stave
docs: docs/multiblocks/silo.md
---
The tower silo turns chopped forage into silage. The grain bin stores grain.

## Tower silo
- Build a ring of **silo staves**, **3 x 3** or **5 x 5** with a hollow middle, up to **24** blocks high, and cap it with **silo roof**.
- Set a **forage blower** (**15 kW**) and a **silo unloader** (**22.4 kW**) beside the bottom ring. Both run on Wildcatter power.

```grid
layer 1
.U.
SSS
S.SB
SSS
layer 2 (repeat up to the top)
SSS
S.S
SSS
layer top
RRR
RRR
RRR
key S farmings:silo_stave
key R farmings:silo_roof
key U farmings:silo_unloader
key B farmings:forage_blower
```

Feed chopped corn or chopped alfalfa into the blower by belt or hopper. One kind at a time.

It holds **240 kg** of dry matter per m³: a **3 x 3** silo **20** blocks high holds **4.8 t** of dry matter, about **14 t** of corn silage.
After the last fill it ferments for **21** real days (**1.4** game days). Then the unloader hands **25 kg** items of silage to a belt,
machine or chest. Some dry matter is lost while it ferments and stands; the readout shows how much.

{item:farmings:chopped_corn} {item:farmings:chopped_alfalfa} {item:farmings:corn_silage} {item:farmings:haylage}

## Grain bin
- A ring of **grain bin wall**, **3 x 3** or **5 x 5**, up to **16** high, with a **grain bin roof**. An auger or belt fills it through the
  roof.
- A **grain bin hopper** beside the foot empties it by gravity, with no power.
- One grain at a time. It holds **721** kg of corn per m³, **412** of oats or **618** of barley. Grain in a bin doesn't spoil.

See [conveyors](conveyors) and [the feed mill](feed-mill).
