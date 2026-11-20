; For VW Golf V (1K), VW Touran (1T), VW Caddy (2K) and Seat Altea (5P), no guaranties
Byte 00 Bit 0 Selective Central Locking
Byte 00 Bit 1 Central Locking without Safe-Function
Byte 00 Bit 2 Central Locking without sensible disable of Safe-Function
Byte 00 Bit 3 Auto-Lock (automatic locking above 10 mph)
Byte 00 Bit 4 Auto-Unlock (unlocking after removing key from ignition)
Byte 00 Bit 5 Central Locking with intentional use of Safe-Function
Byte 00 Bit 6 Central Locking Lock/Unlock-Switch RR/RL activates total locking
Byte 00 Bit 7 Unknown					(RR = Rear Right / RL = Rear Left)
Byte 01 Bit 0 Central Locking (Rear) Right Sliding door installed
Byte 01 Bit 1 Central Locking (Rear) Left Sliding door installed
Byte 01 Bit 2 Central Locking (Tailgate) 2-motors Central Lock installed in place of Central Lock motor
Byte 01 Bit 3 Central Locking (Tailgate) inside switch via CAN
Byte 01 Bit 4 Central Locking (Tailgate) Softtouch (direct open/no release)
Byte 01 Bit 5 Central Locking (Tailgate) switch controlled
Byte 01 Bit 6 Central Locking (Back) Central Locking actuator installed instead of Central Locking Motor
Byte 01 Bit 7 Central Locking (Back) Tailgate within Central Locking
Byte 02 Bit 0 Unknown
Byte 02 Bit 1 Central Locking (Tailgate) "Security Lock"
Byte 02 Bit 2 Central Locking (Rear)			1-Motor- instead of 2-Motors-Lock installed
Byte 02 Bit 3 Central Locking (Tailgate) 		1-Motor- instead of 2-Motors-Lock installed
Byte 02 Bit 4 Central Locking (Back) Central Locking actuator rear window switch controlled
Byte 02 Bit 5 Central Locking (Back) Central Locking actuator for rear window installed (Hatch philosophy A-MPV)
Byte 02 Bit 6 Central Locking with Single door opening, driver door additional
Byte 02 Bit 7 Central Locking (Back) Locking for roof compartment lid and Rear lid cover (Hatch philosophy Cabrio)
Byte 03 Bit 0 Unknown
Byte 03 Bit 1 Unknown
Byte 03 Bit 2 Unknown
Byte 03 Bit 3 Unknown
Byte 03 Bit 4 Unknown
Byte 03 Bit 5 Unknown
Byte 03 Bit 6 Unknown
Byte 03 Bit 7 Unknown
Byte 04 Bit 0 Unknown
Byte 04 Bit 1 Unknown
Byte 04 Bit 2 Unknown
Byte 04 Bit 3 Unknown
Byte 04 Bit 4 Unknown
Byte 04 Bit 5 Unknown
Byte 04 Bit 6 Unknown
Byte 04 Bit 7 Unknown
Byte 05 Bit 0 Unknown
Byte 05 Bit 1 Unknown
Byte 05 Bit 2 Unknown
Byte 05 Bit 3 Unknown
Byte 05 Bit 4 Unknown
Byte 05 Bit 5 Unknown
Byte 05 Bit 6 Unknown
Byte 05 Bit 7 Unknown
Byte 06 Bit 0 Comfort function inactive
Byte 06 Bit 1 Comfort function power windows/sunroof via door lock drivers door inactive
Byte 06 Bit 2 Comfort function power windows/sunroof via door lock passengers door inactive
Byte 06 Bit 3 Comfort function power windows/sunroof via door lock rear lid inactive
Byte 06 Bit 4 Comfort function power windows/sunroof via power window switch in drivers door inactive
Byte 06 Bit 5 Comfort function power windows/sunroof via remote control inactive
Byte 06 Bit 6 Comfort function autom. closing
Byte 06 Bit 7 Power windows/sunroof disabled after door opening
Byte 07 Bit 0 Comfort opening power windows via door lock active
Byte 07 Bit 1 Comfort opening power windows via power window switch active
Byte 07 Bit 2 Comfort opening power windows via remote control active
Byte 07 Bit 3 Comfort closing power windows via door lock active
Byte 07 Bit 4 Comfort closing power windows via power window switch active
Byte 07 Bit 5 Comfort closing power windows via remote control active
Byte 07 Bit 6 Comfort function after authorized access only in a limited time
Byte 07 Bit 7 Not relevant/Unused
Byte 08 Bit 0 Comfort opening sunroof via door lock active
Byte 08 Bit 1 Comfort opening sunroof via power window switch active
Byte 08 Bit 2 Comfort opening sunroof via remote control active
Byte 08 Bit 3 Comfort closing sunroof via door lock active
Byte 08 Bit 4 Comfort closing sunroof via power window switch active
Byte 08 Bit 5 Comfort closing sunroof via remote control active
Byte 08 Bit 6 Comfort opening type for sunroof (not active)	(0 = open	/ 1 = lift up)
Byte 08 Bit 7 Unknown
Byte 09 Bit 0 Unknown
Byte 09 Bit 1 Unknown
Byte 09 Bit 2 Unknown
Byte 09 Bit 3 Unknown
Byte 09 Bit 4 Unknown
Byte 09 Bit 5 Unknown
Byte 09 Bit 6 Unknown
Byte 09 Bit 7 Unknown
Byte 10 Bit 0 Unlocking Confirmation (Blink) via Door Locks
Byte 10 Bit 1 Unlocking Confirmation (Horn) via Door Locks
Byte 10 Bit 2 Unlocking Confirmation (Blink) via Remote Control
Byte 10 Bit 3 Unlocking Confirmation (Horn) via Remote Control
Byte 10 Bit 4 Not relevant/Unused
Byte 10 Bit 5 Not relevant/Unused
Byte 10 Bit 6 Not relevant/Unused
Byte 10 Bit 7 Adaptation of Horn Confirmation locked
Byte 11 Bit 0 Locking Confirmation (Blink) via Door Locks
Byte 11 Bit 1 Locking Confirmation (Horn) via Door Locks
Byte 11 Bit 2 Locking Confirmation (Blink) via Remote Control
Byte 11 Bit 3 Locking Confirmation (Horn) via Remote Control
Byte 11 Bit 4 Not relevant/Unused
Byte 11 Bit 5 Not relevant/Unused
Byte 11 Bit 6 Unknown
Byte 11 Bit 7 Unknown
Byte 12 Bit 0 Unknown
Byte 12 Bit 1 Unknown
Byte 12 Bit 2 Unknown
Byte 12 Bit 3 Unknown
Byte 12 Bit 4 Unknown
Byte 12 Bit 5 Unknown
Byte 12 Bit 6 Unknown
Byte 12 Bit 7 Unknown
Byte 13 Bit 0 Remote control inactive
Byte 13 Bit 1 Remote control with "Terminal 15 ON" inactive
Byte 13 Bit 2 Remote control range limitation active
Byte 13 Bit 3 Manual remote control learning/unlearning active
Byte 13 Bit 4 Not relevant/Unused
Byte 13 Bit 5 Not relevant/Unused
Byte 13 Bit 6 Activation Blink Signal (Anti-Theft-Warning System)
Byte 13 Bit 7 Activation Horn Signal (Anti-Theft-Warning System)
Byte 14 Bit 0 Doors	(0 = 2-doors / 1 = 4-doors)
Byte 14 Bit 1 Power windows front installed
Byte 14 Bit 2 Power windows rear installed
Byte 14 Bit 3 Central locking installed
Byte 14 Bit 4 Model	(0 = Left Hand Drive/ 1 = Right Hand Drive)
Byte 14 Bit 5 Not relevant/Unused
Byte 14 Bit 6 Synchronised mirror adjustment
Byte 14 Bit 7 Mirror lowering with reverse gear (only with Memory Seats)
Byte 15 Bit 0 Anti-theft system deactivated
Byte 15 Bit 1 Anti-theft system disarming only via remote control or immobilizer
Byte 15 Bit 2 Anti-theft system armed with intentional disable of Safe-Function/Interior Monitoring inactive
Byte 15 Bit 3 Anti-theft alarm delay (15 s) inactive (Thatcham)
Byte 15 Bit 4 Not relevant/Unused
Byte 15 Bit 5 Anti-theft system passive arming
Byte 15 Bit 6 Inclination Sensor installed
Byte 15 Bit 7 Interior Monitoring installed
Byte 16 Bit 0 Not relevant/Unused
Byte 16 Bit 1 Not relevant/Unused
Byte 16 Bit 2 Not relevant/Unused
Byte 16 Bit 3 Not relevant/Unused
Byte 16 Bit 4 Alarm Siren installed
Byte 16 Bit 5 Siren Tone Type		(0 = frequency modulated / 1 = intermittent)
Byte 16 Bit 6 Siren Alarm Type	(0 = 10 "tamper" alarms / 1 = 1 "tamper" alarm)
Byte 16 Bit 7 Adaption channel for Siren options locked
Byte 17 Bit 0 Unknown
Byte 17 Bit 1 Unknown
Byte 17 Bit 2 Unknown
Byte 17 Bit 3 Unknown
Byte 17 Bit 4 Unknown
Byte 17 Bit 5 Unknown
Byte 17 Bit 6 Unknown
Byte 17 Bit 7 Unknown
Byte 18 Bit 0 Unknown
Byte 18 Bit 1 Unknown
Byte 18 Bit 2 Unknown
Byte 18 Bit 3 Unknown
Byte 18 Bit 4 Unknown
Byte 18 Bit 5 Unknown
Byte 18 Bit 6 Unknown
Byte 18 Bit 7 Unknown
