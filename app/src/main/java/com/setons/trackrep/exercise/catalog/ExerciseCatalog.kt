package com.setons.trackrep.exercise.catalog

import com.setons.trackrep.camera.ExerciseFramingMode
import com.setons.trackrep.exercise.model.DifficultyLevel
import com.setons.trackrep.exercise.model.EquipmentType
import com.setons.trackrep.exercise.model.Exercise
import com.setons.trackrep.exercise.model.MuscleGroup

object ExerciseCatalog {

    val exercises: List<Exercise> = listOf(
        // ==================== CHEST ====================
        Exercise(
            id = "push_up_wall",
            name = "Wall Push-up",
            targetMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
            difficulty = DifficultyLevel.BEGINNER,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 12,
            restSeconds = 45,
            instructions = listOf(
                "Stand arm's length away from a flat wall with feet hip-width apart.",
                "Place palms flat against the wall at shoulder height and width.",
                "Bend elbows to lower your chest toward the wall with body in a straight plank line.",
                "Press firmly through palms to return to the starting position."
            ),
            commonFlaws = listOf(
                "Arching or sagging the lower back",
                "Shrugging shoulders up toward the ears"
            ),
            proTip = "Step feet further back from the wall to smoothly increase resistance."
        ),
        Exercise(
            id = "push_up_incline",
            name = "Incline Push-up",
            targetMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.ARMS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.NOVICE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 60,
            instructions = listOf(
                "Place hands shoulder-width apart on an elevated, stable surface (couch, bench, or sturdy table).",
                "Step feet back until body forms a continuous straight line from crown to heels.",
                "Lower chest until it touches the edge of the elevated surface.",
                "Push through palms until arms are fully extended at lockout."
            ),
            commonFlaws = listOf(
                "Letting hips drop lower than the torso",
                "Flaring elbows straight out at 90 degrees"
            ),
            proTip = "Keep elbows tucked at a 45-degree angle to protect shoulder joints."
        ),
        Exercise(
            id = "push_up_standard",
            name = "Standard Push-up",
            targetMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.SHOULDERS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.INTERMEDIATE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 60,
            instructions = listOf(
                "Start in a high plank with hands directly beneath shoulders and core braced.",
                "Lower body with control until elbows break 90 degrees (chest ~2 inches off the ground).",
                "Maintain a rigid neutral spine from head to heels throughout the descent.",
                "Press explosively back to full arm extension without hyper-extending."
            ),
            commonFlaws = listOf(
                "Lumbar spine sagging due to loose core engagement",
                "Incomplete elbow bend above 90 degrees",
                "Neck craning forward to reach the floor early"
            ),
            proTip = "AI Vision enabled: Position phone 5-7 feet away side-on for automated angle and rep counting."
        ),
        Exercise(
            id = "push_up_decline",
            name = "Decline Push-up",
            targetMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
            difficulty = DifficultyLevel.ADVANCED,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 8,
            restSeconds = 75,
            instructions = listOf(
                "Place toes up on an elevated bench, chair, or bed frame.",
                "Place hands flat on the floor directly beneath shoulders.",
                "Brace glutes and abs to prevent hip sagging.",
                "Lower chest toward the floor, then press back to lockout."
            ),
            commonFlaws = listOf(
                "Allowing lower back to hyperextend",
                "Rushing the descent without muscle tension"
            ),
            proTip = "Shifts workload heavily to upper clavicular head of the pectorals and anterior deltoids."
        ),
        Exercise(
            id = "push_up_diamond",
            name = "Diamond Push-up",
            targetMuscle = MuscleGroup.ARMS,
            secondaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
            difficulty = DifficultyLevel.ADVANCED,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 8,
            restSeconds = 60,
            instructions = listOf(
                "Assume a push-up position with thumbs and index fingers touching to form a diamond under center chest.",
                "Keep core tight and lower chest directly toward the diamond.",
                "Keep elbows tracking back along the ribs, not flared.",
                "Press up strongly through the triceps."
            ),
            commonFlaws = listOf(
                "Excessive wrist strain from placing diamond too far forward",
                "Flaring elbows excessively wide"
            ),
            proTip = "If wrists are tight, separate thumbs by 1-2 inches while maintaining close hand spacing."
        ),
        Exercise(
            id = "push_up_archer",
            name = "Archer Push-up",
            targetMuscle = MuscleGroup.CHEST,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.ELITE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 6,
            restSeconds = 90,
            instructions = listOf(
                "Set hands significantly wider than shoulder width.",
                "Lower toward one arm, bending that elbow while fully extending the opposite arm outward.",
                "Push back to center and alternate to the other side."
            ),
            commonFlaws = listOf(
                "Twisting hips or torso sideways during descent",
                "Failing to keep the extended arm straight"
            ),
            proTip = "The ultimate unilateral stepping stone to the one-arm push-up."
        ),

        // ==================== QUADS & LOWER BODY ====================
        Exercise(
            id = "squat_chair",
            name = "Chair Box Squat",
            targetMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            difficulty = DifficultyLevel.BEGINNER,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 12,
            restSeconds = 45,
            instructions = listOf(
                "Stand 4-6 inches in front of a sturdy chair or box with feet shoulder-width apart, facing away from the seat.",
                "Brace your core, hinge hips backward, and bend knees while keeping shins near-vertical.",
                "Reach arms forward for counter-balance as glutes lightly tap the seat—do not dump your weight or rock.",
                "Pause momentarily on the seat to eliminate momentum, then drive through heels and midfoot to stand back up."
            ),
            commonFlaws = listOf(
                "Collapsing heavily onto the chair and relaxing core tension",
                "Knees caving inward (valgus collapse) on the ascent",
                "Rocking back and forward to generate momentum instead of leg drive"
            ),
            proTip = "Treat the chair as a tactile depth sensor, not a resting spot. Keep muscular tension constant throughout the touch."
        ),
        Exercise(
            id = "squat_bodyweight",
            name = "Bodyweight Squat",
            targetMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
            difficulty = DifficultyLevel.INTERMEDIATE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 12,
            restSeconds = 60,
            instructions = listOf(
                "Stand tall with feet shoulder-width apart, toes turned outward 10-15 degrees.",
                "Push hips back and bend knees, driving knees outward in line with middle toes.",
                "Extend arms forward in front of chest to maintain balance and an upright torso.",
                "Lower until hip crease drops parallel to or below the top of kneecaps (<=90 deg).",
                "Keep chest proud and drive firmly through heels and midfoot to return to full standing lockout."
            ),
            commonFlaws = listOf(
                "Cutting depth above parallel (half-squatting)",
                "Excessive forward torso pitch (>65 degrees) rounding the lower back",
                "Heels lifting off the floor during the descent"
            ),
            proTip = "AI Vision enabled: Tracks real-time knee flexion angle and alerts you if depth is cut short."
        ),
        Exercise(
            id = "lunge_reverse",
            name = "Reverse Lunge",
            targetMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            difficulty = DifficultyLevel.NOVICE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 60,
            instructions = listOf(
                "Stand tall with feet together and hands on hips.",
                "Step one leg backward, dropping the back knee until it hovers 1 inch above the floor.",
                "Keep the front knee aligned directly over the front ankle at 90 degrees.",
                "Drive through the front heel to return to the starting standing position."
            ),
            commonFlaws = listOf(
                "Front knee tracking past toes or collapsing inward",
                "Torso leaning heavily forward over the front thigh"
            ),
            proTip = "Reverse lunges place significantly less shearing force on kneecaps than forward lunges."
        ),
        Exercise(
            id = "squat_jump",
            name = "Jump Squat",
            targetMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES),
            difficulty = DifficultyLevel.ADVANCED,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 75,
            instructions = listOf(
                "Stand with feet shoulder-width apart and arms relaxed at your sides.",
                "Hinge hips and bend knees into a loaded squat (parallel depth), sweeping arms slightly back.",
                "Explode vertically off the floor with maximal power, driving arms upward for vertical propulsion.",
                "Achieve full triple extension (hips, knees, and ankles straight) at peak height in mid-air.",
                "Land quietly and softly on balls of feet, immediately absorbing the descent by sinking into the next squat."
            ),
            commonFlaws = listOf(
                "Landing stiff-legged with knees locked, sending impact shock through joints",
                "Slapping feet loudly on the floor instead of absorbing softly through hips and quads",
                "Cutting squat depth before launching into the jump"
            ),
            proTip = "Aim for maximum vertical height and silent landings. If your landing makes a loud thud, absorb deeper through the knees."
        ),
        Exercise(
            id = "squat_bulgarian",
            name = "Bulgarian Split Squat",
            targetMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            difficulty = DifficultyLevel.ADVANCED,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 8,
            restSeconds = 60,
            instructions = listOf(
                "Stand 2-3 feet in front of a sturdy bench, chair, or box (ideal height is mid-shin).",
                "Reach one leg back and place the top of the foot (laces down) flat on the bench.",
                "Keep torso tall with a slight athletic forward pitch (~15-20°) and core engaged.",
                "Lower hips straight down until front thigh is parallel to the ground and front knee is at ~90 degrees.",
                "Ensure front shin stays nearly vertical; back knee hovers 1-2 inches above the floor.",
                "Drive forcefully through the front heel and midfoot to return to the top lockout."
            ),
            commonFlaws = listOf(
                "Standing too close to the bench, pushing front knee excessively past toes and lifting the front heel",
                "Allowing the front knee to wobble or cave inward",
                "Excessive hyperextension of the lumbar spine (arching lower back)"
            ),
            proTip = "Keep 85% of your weight on the front working leg. The rear elevated foot is strictly for balance."
        ),
        Exercise(
            id = "squat_pistol",
            name = "Pistol Squat",
            targetMuscle = MuscleGroup.QUADS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CORE, MuscleGroup.CALVES),
            difficulty = DifficultyLevel.ELITE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 5,
            restSeconds = 90,
            instructions = listOf(
                "Stand tall balanced on one leg with core braced and shoulders engaged.",
                "Extend the non-working leg straight out in front, parallel to the ground with toes pointed forward.",
                "Reach both arms straight out in front of your chest to provide essential counterweight balance.",
                "Bend the standing knee and push hips back, descending under full control until hamstring touches calf.",
                "Keep the extended non-working leg completely off the floor throughout the entire movement.",
                "Drive hard through the heel and midfoot of the standing leg, maintaining chest height, to stand back up to full lockout."
            ),
            commonFlaws = listOf(
                "Standing heel lifting off the ground due to tight ankles",
                "Extended non-working leg drooping and touching the floor",
                "Collapsing the chest forward into severe spinal rounding"
            ),
            proTip = "Hold a light object or reach forward with both fists as a counterweight to make balancing significantly easier."
        ),

        // ==================== CORE & ABS ====================
        Exercise(
            id = "plank_knee",
            name = "Knee Plank",
            targetMuscle = MuscleGroup.CORE,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS),
            difficulty = DifficultyLevel.BEGINNER,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 0,
            defaultHoldSeconds = 30,
            restSeconds = 45,
            instructions = listOf(
                "Rest forearms on the floor with elbows directly beneath shoulders.",
                "Rest knees on the floor with hips aligned in a straight diagonal line from knees to head.",
                "Squeeze glutes and draw navel toward spine.",
                "Hold continuously while breathing rhythmically."
            ),
            commonFlaws = listOf(
                "Pushing hips back toward heels",
                "Holding breath"
            ),
            proTip = "Perfect starting point to master transverse abdominis contraction before full planks."
        ),
        Exercise(
            id = "plank_standard",
            name = "Standard Forearm Plank",
            targetMuscle = MuscleGroup.CORE,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.GLUTES),
            difficulty = DifficultyLevel.INTERMEDIATE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 0,
            defaultHoldSeconds = 45,
            restSeconds = 60,
            instructions = listOf(
                "Place elbows directly beneath shoulders with forearms flat and parallel.",
                "Extend legs straight back, resting on toes.",
                "Maintain a rigid straight plane from crown to heels (180 degrees).",
                "Squeeze quads, glutes, and abdominals continuously."
            ),
            commonFlaws = listOf(
                "Lumbar hip sagging toward the floor",
                "Hips piking upward to alleviate abdominal burn"
            ),
            proTip = "AI Vision enabled: Distinguishes between hip sag and hip pike with voice milestones every 15s."
        ),
        Exercise(
            id = "plank_side",
            name = "Side Plank",
            targetMuscle = MuscleGroup.CORE,
            secondaryMuscles = listOf(MuscleGroup.SHOULDERS, MuscleGroup.GLUTES),
            difficulty = DifficultyLevel.NOVICE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 0,
            defaultHoldSeconds = 30,
            restSeconds = 45,
            instructions = listOf(
                "Lie on your side with legs straight and feet stacked.",
                "Place lower elbow directly under the shoulder.",
                "Lift hips off the floor until body forms a straight diagonal line.",
                "Hold and keep the top hip rolled slightly forward."
            ),
            commonFlaws = listOf(
                "Allowing bottom hip to sag toward the floor",
                "Rolling chest backward"
            ),
            proTip = "Critical for lateral core stability and preventing spinal rotation injuries."
        ),
        Exercise(
            id = "hollow_body_hold",
            name = "Hollow Body Hold",
            targetMuscle = MuscleGroup.CORE,
            secondaryMuscles = listOf(MuscleGroup.QUADS),
            difficulty = DifficultyLevel.ADVANCED,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 0,
            defaultHoldSeconds = 30,
            restSeconds = 60,
            instructions = listOf(
                "Lie on back with arms extended overhead and legs straight.",
                "Press the entire lower back flat into the floor (zero gap).",
                "Lift shoulder blades and heels 2-3 inches off the ground.",
                "Maintain this banana-shaped posture with tight abdominal tension."
            ),
            commonFlaws = listOf(
                "Lower back arching off the floor",
                "Bending knees or relaxing neck"
            ),
            proTip = "Gold standard gymnastic core exercise for anterior pelvic control."
        ),
        Exercise(
            id = "v_ups",
            name = "V-Ups",
            targetMuscle = MuscleGroup.CORE,
            secondaryMuscles = listOf(MuscleGroup.QUADS),
            difficulty = DifficultyLevel.ADVANCED,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 60,
            instructions = listOf(
                "Lie flat on the floor with arms extended overhead and legs straight.",
                "Simultaneously lift torso and straight legs, reaching fingertips toward toes.",
                "Balance on the tailbone at the peak forming a 'V' shape.",
                "Lower under control back to the starting position."
            ),
            commonFlaws = listOf(
                "Using arm momentum rather than abdominal contraction",
                "Bending knees significantly"
            ),
            proTip = "Synchronize the lift: chest and legs should reach the peak at the exact same moment."
        ),
        Exercise(
            id = "dragon_flag",
            name = "Dragon Flag",
            targetMuscle = MuscleGroup.CORE,
            secondaryMuscles = listOf(MuscleGroup.BACK, MuscleGroup.ARMS),
            difficulty = DifficultyLevel.ELITE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 5,
            restSeconds = 90,
            instructions = listOf(
                "Lie on back holding a sturdy anchor or bench behind your head.",
                "Lift entire body up vertically onto your shoulder blades.",
                "Lower straight body slowly in one rigid line without bending at the hips.",
                "Reverse just before touching the ground and lift back up."
            ),
            commonFlaws = listOf(
                "Bending at the waist instead of maintaining a straight line",
                "Dropping hips heavily"
            ),
            proTip = "Made legendary by Bruce Lee; requires total body isometric bracing."
        ),

        // ==================== GLUTES & HAMSTRINGS ====================
        Exercise(
            id = "glute_bridge",
            name = "Glute Bridge",
            targetMuscle = MuscleGroup.GLUTES,
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.BEGINNER,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 15,
            restSeconds = 45,
            instructions = listOf(
                "Lie flat on back with knees bent and feet flat on the floor hip-width apart.",
                "Drive through heels to lift hips toward ceiling until thighs and torso align.",
                "Squeeze glutes hard at the top for 1 full second.",
                "Lower hips slowly back to the floor."
            ),
            commonFlaws = listOf(
                "Overarching the lower back at the top",
                "Pushing through toes instead of heels"
            ),
            proTip = "Wake up dormant glutes and combat the posture fatigue of prolonged sitting."
        ),
        Exercise(
            id = "single_leg_bridge",
            name = "Single-Leg Glute Bridge",
            targetMuscle = MuscleGroup.GLUTES,
            secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.INTERMEDIATE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 60,
            instructions = listOf(
                "Set up in a glute bridge position, then lift one leg straight into the air.",
                "Drive through the heel of the grounded foot to lift hips.",
                "Keep pelvis square without tilting toward the free leg.",
                "Lower slowly and repeat before switching sides."
            ),
            commonFlaws = listOf(
                "Pelvis dipping on the unsupported side",
                "Cramping hamstring due to placing foot too far forward"
            ),
            proTip = "Place the grounded foot close to hips to maximize glute contraction over hamstrings."
        ),
        Exercise(
            id = "good_mornings",
            name = "Bodyweight Good Morning",
            targetMuscle = MuscleGroup.HAMSTRINGS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.BACK),
            difficulty = DifficultyLevel.NOVICE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 12,
            restSeconds = 45,
            instructions = listOf(
                "Stand tall with feet hip-width apart and hands behind head.",
                "Hinge at hips, pushing hips back while keeping knees soft and back flat.",
                "Lower torso until you feel a deep stretch in the hamstrings.",
                "Drive hips forward to return to standing."
            ),
            commonFlaws = listOf(
                "Rounding the lumbar spine",
                "Squatting by bending knees excessively"
            ),
            proTip = "Think of reaching back with your tailbone to touch an imaginary wall behind you."
        ),
        Exercise(
            id = "nordic_curl",
            name = "Nordic Hamstring Curl",
            targetMuscle = MuscleGroup.HAMSTRINGS,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES),
            difficulty = DifficultyLevel.ELITE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 5,
            restSeconds = 90,
            instructions = listOf(
                "Kneel on a soft pad with feet securely anchored under a couch or heavy weight.",
                "Keep hips extended and torso in a rigid line.",
                "Lower body forward toward the floor as slowly as possible using hamstring resistance.",
                "Catch yourself with hands on the floor and push back up."
            ),
            commonFlaws = listOf(
                "Bending at the hips during the fall",
                "Failing to control the eccentric descent"
            ),
            proTip = "The single most effective exercise for hamstring injury prevention and sprinting speed."
        ),

        // ==================== CALVES ====================
        Exercise(
            id = "calf_raise_double",
            name = "Standing Calf Raise",
            targetMuscle = MuscleGroup.CALVES,
            secondaryMuscles = emptyList(),
            difficulty = DifficultyLevel.BEGINNER,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 20,
            restSeconds = 30,
            instructions = listOf(
                "Stand tall on flat ground or the edge of a step with feet hip-width apart.",
                "Rise up high onto the balls of your feet, squeezing calf muscles at the peak.",
                "Pause for 1 second at the top, then lower heels slowly below the step line."
            ),
            commonFlaws = listOf(
                "Bouncing quickly without holding peak contraction",
                "Rolling weight onto outer toes"
            ),
            proTip = "Press through the big toe knuckle to engage the full gastrocnemius medial head."
        ),
        Exercise(
            id = "calf_raise_single",
            name = "Single-Leg Calf Raise",
            targetMuscle = MuscleGroup.CALVES,
            secondaryMuscles = emptyList(),
            difficulty = DifficultyLevel.INTERMEDIATE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 12,
            restSeconds = 45,
            instructions = listOf(
                "Stand on one leg on the edge of a step, lightly holding a wall for balance.",
                "Lower heel into a full stretch below step level.",
                "Drive forcefully onto the ball of your foot into full plantar flexion.",
                "Hold peak for 1 second, then lower under a 2-second count."
            ),
            commonFlaws = listOf(
                "Using hand balance to pull body up",
                "Skipping the bottom stretch"
            ),
            proTip = "Doubles the effective bodyweight load on each calf."
        ),

        // ==================== SHOULDERS ====================
        Exercise(
            id = "arm_circles_scapular",
            name = "Scapular Wall Squeeze & Circles",
            targetMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.BACK),
            difficulty = DifficultyLevel.BEGINNER,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.SQUAT,
            defaultSets = 3,
            defaultReps = 15,
            restSeconds = 30,
            instructions = listOf(
                "Stand tall with arms extended out to the sides at shoulder height.",
                "Draw shoulder blades down and together.",
                "Perform controlled forward circles for half the reps, then reverse backwards."
            ),
            commonFlaws = listOf(
                "Shrugging shoulders into the neck",
                "Arched lower back"
            ),
            proTip = "Excellent warm-up activator for the rotator cuff and serratus anterior."
        ),
        Exercise(
            id = "pike_push_up",
            name = "Pike Push-up",
            targetMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.INTERMEDIATE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 8,
            restSeconds = 60,
            instructions = listOf(
                "Assume a downward dog position with hips high in the air forming an inverted 'V'.",
                "Look back toward feet and shift weight onto hands.",
                "Bend elbows to lower crown of head slightly forward of hands toward the floor.",
                "Push back through shoulders and arms to the pike position."
            ),
            commonFlaws = listOf(
                "Flaring elbows wide instead of tracking back at 45 degrees",
                "Dropping hips into a standard push-up"
            ),
            proTip = "Primary bodyweight movement for overhead vertical pressing power."
        ),
        Exercise(
            id = "handstand_hold",
            name = "Wall Handstand Hold",
            targetMuscle = MuscleGroup.SHOULDERS,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.ELITE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 0,
            defaultHoldSeconds = 30,
            restSeconds = 90,
            instructions = listOf(
                "Face a wall, place hands 6-8 inches away and kick up into a handstand.",
                "Push the floor away actively through shoulders, locking arms straight.",
                "Brace core and keep feet together touching the wall lightly.",
                "Hold position while breathing smoothly."
            ),
            commonFlaws = listOf(
                "Banana-shaped back from loose core",
                "Bending elbows under fatigue"
            ),
            proTip = "Push through fingertips to control balance and build iron deltoids."
        ),

        // ==================== ARMS (TRICEPS & BICEPS) ====================
        Exercise(
            id = "bench_dips",
            name = "Chair / Bench Dips",
            targetMuscle = MuscleGroup.ARMS,
            secondaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
            difficulty = DifficultyLevel.NOVICE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 12,
            restSeconds = 60,
            instructions = listOf(
                "Sit on the edge of a sturdy chair, coffee table, or bench with palms gripping the front edge beside your hips.",
                "Slide hips forward off the seat, supporting your weight on your hands with feet planted out in front.",
                "Bend elbows backward, lowering hips vertically until upper arms are parallel to the floor (elbows at 90 degrees).",
                "Keep back gliding within inches of the chair edge—do not drift forward.",
                "Press forcefully through palms to extend arms and lock out triceps at the top."
            ),
            commonFlaws = listOf(
                "Dipping below 90 degrees, causing severe anterior shoulder impingement",
                "Drifting hips too far away from the chair, straining rotator cuffs",
                "Shrugging shoulders up toward the ears"
            ),
            proTip = "Keep your back gliding close to the front edge of the chair to keep tension strictly on the triceps."
        ),
        Exercise(
            id = "close_grip_push_up",
            name = "Close-Grip Push-up",
            targetMuscle = MuscleGroup.ARMS,
            secondaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.CORE),
            difficulty = DifficultyLevel.INTERMEDIATE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PUSH_UP,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 60,
            instructions = listOf(
                "Assume a push-up position with hands placed directly under the sternum, thumbs 6 inches apart.",
                "Lower chest while grazing elbows along your ribs.",
                "Press forcefully up to lock out triceps."
            ),
            commonFlaws = listOf(
                "Flaring elbows outward",
                "Sagging hips"
            ),
            proTip = "Isolates the lateral and medial heads of the triceps with high hypertrophy stimulus."
        ),

        // ==================== BACK & POSTERIOR ====================
        Exercise(
            id = "bird_dog",
            name = "Bird Dog",
            targetMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.GLUTES),
            difficulty = DifficultyLevel.BEGINNER,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 45,
            instructions = listOf(
                "Start on hands and knees with wrists under shoulders and knees under hips.",
                "Reach right arm straight forward and left leg straight back simultaneously.",
                "Hold parallel to the floor for 1 second with hips and shoulders square.",
                "Return to starting position and switch to the opposite side."
            ),
            commonFlaws = listOf(
                "Rotating hips to reach higher",
                "Arching lower back"
            ),
            proTip = "One of Dr. Stuart McGill's 'Big 3' exercises for low-back stability and health."
        ),
        Exercise(
            id = "superman_hold",
            name = "Prone Superman Hold",
            targetMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.SHOULDERS),
            difficulty = DifficultyLevel.NOVICE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PLANK,
            defaultSets = 3,
            defaultReps = 12,
            restSeconds = 45,
            instructions = listOf(
                "Lie face down on the floor with arms outstretched overhead and legs straight.",
                "Simultaneously lift chest, arms, and legs 2-4 inches off the floor.",
                "Squeeze erector spinae, glutes, and upper back at the peak for 2 seconds.",
                "Lower gently back to the floor."
            ),
            commonFlaws = listOf(
                "Craning neck backward excessively",
                "Jerking up with momentum instead of muscular contraction"
            ),
            proTip = "Keep chin tucked in a double-chin posture to maintain neutral cervical alignment."
        ),
        Exercise(
            id = "inverted_row",
            name = "Table / Inverted Row",
            targetMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.INTERMEDIATE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.PULL_UP,
            defaultSets = 3,
            defaultReps = 8,
            restSeconds = 60,
            instructions = listOf(
                "Lie under a sturdy table, gripping the edge with an overhand grip wider than shoulders.",
                "Keep heels on the floor and body in a rigid reverse plank.",
                "Pull chest upward toward the table edge, squeezing shoulder blades together.",
                "Lower slowly under control to full arm extension."
            ),
            commonFlaws = listOf(
                "Sagging hips",
                "Initiating pull with arms rather than retracting scapulae"
            ),
            proTip = "Crucial horizontal pulling movement for posture balance and scapular retraction."
        ),
        Exercise(
            id = "pull_up_standard",
            name = "Strict Pull-up",
            targetMuscle = MuscleGroup.BACK,
            secondaryMuscles = listOf(MuscleGroup.ARMS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.ADVANCED,
            equipment = EquipmentType.PULLUP_BAR,
            framingMode = ExerciseFramingMode.PULL_UP,
            defaultSets = 3,
            defaultReps = 6,
            restSeconds = 90,
            instructions = listOf(
                "Hang from an overhead bar with an overhand grip slightly wider than shoulder width.",
                "Pull shoulder blades down and back, engaging lats.",
                "Drive elbows down toward hips until chin clears the bar.",
                "Lower with complete control to a dead hang."
            ),
            commonFlaws = listOf(
                "Kicking legs or kipping to generate momentum",
                "Failing to descend to a complete dead hang"
            ),
            proTip = "The gold standard upper body vertical pull."
        ),

        // ==================== FULL BODY / CONDITIONING ====================
        Exercise(
            id = "mountain_climber",
            name = "Mountain Climber",
            targetMuscle = MuscleGroup.FULL_BODY,
            secondaryMuscles = listOf(MuscleGroup.CORE, MuscleGroup.SHOULDERS, MuscleGroup.QUADS),
            difficulty = DifficultyLevel.NOVICE,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.CARDIO,
            defaultSets = 3,
            defaultReps = 20,
            restSeconds = 45,
            instructions = listOf(
                "Begin in a high plank position with shoulders stacked directly over wrists.",
                "Drive one knee toward your chest without letting hips pike into the air.",
                "Quickly switch legs in a running motion, maintaining a flat back throughout."
            ),
            commonFlaws = listOf(
                "Bouncing hips up and down",
                "Hands drifting out in front of shoulders"
            ),
            proTip = "Focus on keeping hips steady rather than just moving feet quickly."
        ),
        Exercise(
            id = "burpee_standard",
            name = "Full Burpee",
            targetMuscle = MuscleGroup.FULL_BODY,
            secondaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.QUADS, MuscleGroup.CORE),
            difficulty = DifficultyLevel.ADVANCED,
            equipment = EquipmentType.BODYWEIGHT,
            framingMode = ExerciseFramingMode.CARDIO,
            defaultSets = 3,
            defaultReps = 10,
            restSeconds = 75,
            instructions = listOf(
                "From standing, drop into a squat and place hands on the floor.",
                "Jump feet back into a high plank and immediately lower chest to the floor.",
                "Push up and jump feet back in toward hands.",
                "Explode vertically into a jump with hands clapping overhead."
            ),
            commonFlaws = listOf(
                "Sagging lower back on the plank drop",
                "Landing stiffly on heels"
            ),
            proTip = "Keep a steady cadence: smooth pacing outperforms erratic sprinting."
        )
    )

    fun getAll(): List<Exercise> = exercises

    fun getById(id: String): Exercise? = exercises.firstOrNull { it.id == id }

    fun getByMuscle(muscle: MuscleGroup): List<Exercise> {
        return exercises.filter { it.targetMuscle == muscle || it.secondaryMuscles.contains(muscle) }
    }

    fun getByDifficulty(difficulty: DifficultyLevel): List<Exercise> {
        return exercises.filter { it.difficulty == difficulty }
    }

    fun getByEquipment(equipment: EquipmentType): List<Exercise> {
        return exercises.filter { it.equipment == equipment }
    }

    fun getVisionSupported(): List<Exercise> {
        return exercises.filter { it.isVisionSupported }
    }

    fun search(query: String): List<Exercise> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return exercises
        return exercises.filter {
            it.name.lowercase().contains(q) ||
            it.targetMuscle.displayName.lowercase().contains(q) ||
            it.difficulty.displayName.lowercase().contains(q) ||
            it.secondaryMuscles.any { sm -> sm.displayName.lowercase().contains(q) } ||
            it.equipment.displayName.lowercase().contains(q)
        }
    }
}
