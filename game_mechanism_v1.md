## Game Title
Princess Star Adventure

## Target players
12 year old girls

## Game Type
Level based platform action game. For the arangement of the platforms you can think about Super Mario.

## Gameplay view
Landscape view of the phone (phone being hold horizontally)

## Game mechanism
### Princess actions along with animation and sound effects:
#### Idle
- Animation: frames in assets/images/PrincessStarAdventure/princess/standing/
#### Single Jump: Triggered by pressing the jump button.
- Animation: frames in assets/images/PrincessStarAdventure/princess/jumping/
- Sound effect: None. 
#### Double Jump: Triggered by pressing the jump button during the single jump phase.
- Animation: Use same animation as for Single Jump.
- Sound effect: assets/sounds/PrincessStarAdventure/princess/princess double jump.wav
#### Normal Attack: Triggered by pressing the attack button, shoot projectiles with magic wand.
- Animation: frames in assets/images/PrincessStarAdventure/princess/attacking/
- Sound effect: assets/sounds/PrincessStarAdventure/princess/star-attack.wav
#### Projectile of the Normal Attack
- Animation: assets/images/PrincessStarAdventure/projectiles/star/star-projectile.png
- Sound: None
#### Gets hurt: if an enemy or enemy's projectile touches her, she loses 1 health point.
- Animation: assets/images/PrincessStarAdventure/princess/hurt/01.png
- Sound: assets/sounds/PrincessStarAdventure/princess/hurt.wav



### Princess health
- One Health point is represented by one heart icon: 
- Princess starts with 2 health points.

### Enemy type 1: teacup-sentry actions along with animation and sound effects:
#### Walk: its default behavior, just walk left and right on the platform
- Animation: frames in assets/images/PrincessStarAdventure/enemies/teacup-sentry/walking/
- Sound: None.
#### Gets hurt once: just show the image of it being hurt
- Animation: assets/images/PrincessStarAdventure/enemies/teacup-sentry/hit/01.png
- Sound: None.
#### Gets hurt twice: the teacup sentry is destroyed. Player gains 1 point.
- Animation: assets/images/PrincessStarAdventure/enemies/teacup-sentry/destroyed/01.png
- Sound: assets/sounds/PrincessStarAdventure/enemies/teacup/teacup-crash.wav

### Score:
- The in-level HUD score shows only points gathered by destroying enemies.
- Each teacup-sentry's destruction grants 1 point.
- If the player does not finish the level, the level score is only the enemy destruction score gathered so far. No time bonus is awarded.
- If the player finishes the level, add a finish-time bonus to the enemy destruction score.
- Count the time player needed to finish the level. The finish-time bonus is based on the remaining time from a 5 minute limit. Convert the remaining time into seconds, divide by 10, and round after division. If 5 minutes - player time < 0, then round it to 0.

### Losing:
- Princess' health points recude to 0.
- Losing message: show a suitable losing message with suitalbe background design. The losing message should include the summed Score accumulated so far. 

### Winning:
- In a level, the level is won if the princess reaches the goal.
- The score of each passed level should be stored and accumulated into next level.
- Winning message: After the player passes a level, show an cheerful graph to show Victory, 
  display the Score gained in that level. Display the scored gained from enemy desctruction and timing both seperately and the combined summed score in that level. And show also othe accumulated total score gained from all passed levels so far.


## Level design:
1. Level 1: 
- Goal: is to reach the castle, represented by assets/images/PrincessStarAdventure/castle/castle.png
- Background music: assets/music/ChasingLight.mp3
- Background image: assets/images/PrincessStarAdventure/backgrounds/Level 1.png
- Platform sprites: assets\images\PrincessStarAdventure\platforms\level1

## HUD & UI:
1. Control:
- Use assets/images/PrincessStarAdventure/ui/mobile-left-button.png to move left.
- Use assets/images/PrincessStarAdventure/ui/mobile-right-button.png to move right.
- Use assets/images/PrincessStarAdventure/ui/mobile-attack-button.png to shoot projectile.
- Use assets/images/PrincessStarAdventure/ui/mobile-jump-button.png to jump. If pressed again during the first single jump phase, then double jump is triggered. There is nothing more than double jump.
2. HUD:
- Display only enemy destruction points gathered in that level at top center during gameplay.
- Display elapsed time in the top right corner
- Make sure these HUD elements have pleasent design appealing to little girls.

