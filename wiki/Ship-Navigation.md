# Ship Navigation

The ship navigation interface is opened from a ship core or linked ship controller. It provides a starmap, destination browser, ship status, dimension editor, movement preview, and drive controls in one screen.

## Map

- The map shows configured celestial bodies in their parent coordinate space.
- Space is treated as the containing backdrop for planets and other local bodies, not as a body at the ship location.
- The ship marker uses the ship core X/Z position while in space or hyperspace.
- Celestial body tooltips show object id, type, and X/Z coordinates.

## Destinations

The Destinations tab lists reachable objects and includes:

- current location marker
- destination X/Z coordinates
- estimated leg count
- estimated energy
- estimated travel time
- effective first-leg range

Routes can include takeoff, orbital approach, landing, hyperspace entry, hyperspace cruise, and hyperspace exit. The displayed range is calculated from the same movement preview used by the drive.

## Autopilot Modes

- Off: no route chaining.
- Assisted: requires confirmation for each leg.
- Safety stops: continues through safe legs and pauses before riskier transitions.
- Full auto: continues all route legs until arrival, energy wait, pause, or failure.

Full auto preserves the engaged target through ship movement so the moved core can continue the route after each jump completes.

## Ship Bounds

The Dimensions tab edits front, back, left, right, up, and down bounds. Valid edits are applied automatically. Invalid or incomplete values remain local to the text field until they become valid.

## Movement Range

The drive clamps movement to the calculated maximum distance for the current ship, command, mass, and movement type. This range comes from the ship movement cost calculation and configured movement formulas. Defensive input limits only reject pathological values from GUI, computer, or NBT input; they are not the gameplay range.

## Troubleshooting

- If the route stops in assisted mode, press Engage to confirm the next leg.
- If the route stops in safety-stops mode, check whether the next leg requires confirmation.
- If full auto stops unexpectedly, check the drive status and blocker text for energy, cooldown, stale scan, invalid route, or movement failure.
- If map and destination coordinates do not match expectations, confirm the celestial object parent coordinates in the active celestial object configuration.
