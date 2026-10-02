package com.sonnik.topower.data

/**
 * Техника выполнения упражнений по умолчанию: по три пункта — исходное положение, движение,
 * главная ошибка. Ключ — русское название упражнения.
 */
val TECHNIQUE: Map<String, Pair<String, String>> = mapOf(
    "Отжимания на упорах" to Pair(
        "• Упоры чуть шире плеч, тело — прямая линия от головы до пяток, пресс и ягодицы напряжены.\n• Опускайтесь, пока грудь не окажется ниже уровня рук, локти под углом около 45° к корпусу.\n• Выжимайте себя вверх, не прогибаясь в пояснице и не задирая таз.",
        "• Handles slightly wider than shoulders, body in a straight line from head to heels, abs and glutes tight.\n• Lower until the chest is below the hands, elbows about 45° from the torso.\n• Press up without sagging the lower back or piking the hips.",
    ),
    "Разводка гантелей на скамье" to Pair(
        "• Лёжа на скамье, гантели над грудью, ладони смотрят друг на друга, локти слегка согнуты и не меняют угол.\n• Разводите руки по дуге до лёгкого растяжения груди — не ниже уровня плеч.\n• Сводите по той же дуге, будто обнимаете бочку; лопатки сведены и прижаты к скамье.",
        "• Lie on the bench, dumbbells over the chest, palms facing, elbows slightly bent and locked at that angle.\n• Open the arms in an arc until a light chest stretch — not below shoulder level.\n• Bring them back along the same arc as if hugging a barrel; shoulder blades squeezed into the bench.",
    ),
    "Резинка на перекладине, на трицепс" to Pair(
        "• Закрепите резину на перекладине, встаньте лицом к ней, локти прижаты к бокам.\n• Разгибайте руки вниз до полного выпрямления — двигаются только предплечья.\n• Медленно возвращайте до угла около 90° в локте, не отводя локти вперёд.",
        "• Anchor the band to the bar, face it, elbows pinned to your sides.\n• Push down until the arms are fully straight — only the forearms move.\n• Return slowly to about 90° at the elbow without letting the elbows drift forward.",
    ),
    "Резинка на трицепс из-за спины" to Pair(
        "• Закрепите резину внизу сзади или наступите на неё; руки с концами резины за головой, локти смотрят вверх.\n• Разгибайте руки над головой, плечи неподвижны, локти не разводите.\n• Опускайте медленно за голову до растяжения трицепса, поясницу не прогибайте.",
        "• Anchor the band low behind you or stand on it; hands behind the head, elbows pointing up.\n• Extend the arms overhead, upper arms still, elbows not flaring.\n• Lower slowly behind the head to a triceps stretch without arching the lower back.",
    ),
    "Тяга Ли Хейни" to Pair(
        "• Вес за спиной на прямых руках, хват чуть шире бёдер, корпус прямой.\n• Тяните вес вверх вдоль спины: локти уходят назад и вверх, лопатки сводятся.\n• Короткая пауза наверху и медленный спуск; не наклоняйтесь вперёд и не помогайте ногами.",
        "• Weight behind your back on straight arms, grip slightly wider than the hips, torso upright.\n• Pull the weight up along the back: elbows go back and up, shoulder blades squeeze.\n• Pause at the top and lower slowly; don't lean forward or use the legs.",
    ),
    "Молотки с гантелями" to Pair(
        "• Стоя, гантели в опущенных руках, ладони смотрят друг на друга.\n• Сгибайте руки, сохраняя хват, локти прижаты к бокам и не уходят вперёд.\n• Опускайте медленно до полного выпрямления, корпус не раскачивайте.",
        "• Stand with dumbbells at your sides, palms facing each other.\n• Curl while keeping the grip, elbows pinned to the sides.\n• Lower slowly to full extension without swinging.",
    ),
    "Молоточек с упором в бедро" to Pair(
        "• Сидя, локоть рабочей руки упирается во внутреннюю часть бедра, хват нейтральный.\n• Сгибайте руку до верхней точки, плечо остаётся неподвижным.\n• Опускайте медленно почти до полного выпрямления, не отрывая локоть от бедра.",
        "• Seated, working elbow braced against the inner thigh, neutral grip.\n• Curl to the top while the upper arm stays still.\n• Lower slowly to almost straight without lifting the elbow off the thigh.",
    ),
    "Подтягивания узким обратным хватом" to Pair(
        "• Хват снизу на ширине плеч или уже, повисните на прямых руках, лопатки опущены.\n• Тянитесь грудью к перекладине, локти идут вниз вдоль корпуса, подбородок выше перекладины.\n• Опускайтесь подконтрольно до прямых рук — без рывков и раскачки.",
        "• Underhand grip shoulder-width or closer, hang on straight arms, shoulder blades down.\n• Pull the chest to the bar, elbows travel down along the body, chin over the bar.\n• Lower under control to straight arms — no kipping or swinging.",
    ),
    "Подтягивания прямым широким хватом" to Pair(
        "• Хват сверху заметно шире плеч, повисните, опустите и сведите лопатки.\n• Тянитесь верхом груди к перекладине, локти идут вниз и в стороны.\n• Опускайтесь медленно до прямых рук; не раскачивайтесь и не помогайте ногами.",
        "• Overhand grip clearly wider than shoulders; hang, then set the shoulder blades down and back.\n• Pull the upper chest toward the bar, elbows travel down and out.\n• Lower slowly to straight arms; no swinging or leg drive.",
    ),
    "Средняя дельта с эспандером" to Pair(
        "• Встаньте на резину, концы в руках у бёдер, локти слегка согнуты.\n• Поднимайте руки через стороны до уровня плеч — ведёт локоть, кисть не выше локтя.\n• Опускайте медленно, не раскачивая корпус и не поднимая плечи к ушам.",
        "• Stand on the band, ends in hands at the hips, elbows slightly bent.\n• Raise the arms out to shoulder height — lead with the elbow, hands no higher than elbows.\n• Lower slowly without swinging the torso or shrugging.",
    ),
    "Пресс с роликом" to Pair(
        "• На коленях, ролик под плечами, спина слегка скруглена, пресс напряжён.\n• Катите ролик вперёд, пока удаётся держать поясницу без прогиба.\n• Возвращайтесь усилием пресса, а не рук; начинайте с короткой амплитуды.",
        "• Kneel with the wheel under the shoulders, back slightly rounded, abs braced.\n• Roll forward only as far as you can keep the lower back from sagging.\n• Come back with the abs, not the arms; start with a short range.",
    ),
    "Планка" to Pair(
        "• Упор на предплечья, локти под плечами, тело — прямая линия.\n• Напрягите пресс и ягодицы, таз не опускайте и не поднимайте.\n• Дышите ровно, взгляд в пол; заканчивайте, когда форма начинает ломаться.",
        "• Forearms on the floor, elbows under shoulders, body in a straight line.\n• Brace abs and glutes; hips neither sag nor pike.\n• Breathe evenly, look at the floor; stop when the form starts to break.",
    ),
    "Прыжки на скакалке" to Pair(
        "• Локти у корпуса, вращайте скакалку кистями, а не плечами.\n• Прыгайте невысоко на передней части стопы, колени слегка согнуты.\n• Держите ровный ритм и прямую спину, приземляйтесь мягко.",
        "• Elbows close to the body, turn the rope with the wrists, not the shoulders.\n• Jump low on the balls of the feet, knees slightly bent.\n• Keep a steady rhythm and an upright back; land softly.",
    ),
    "Жим гантелей лёжа" to Pair(
        "• Лёжа на скамье, лопатки сведены, стопы упираются в пол, гантели над грудью.\n• Опускайте гантели к нижней части груди, локти под углом около 45–60° к корпусу.\n• Выжимайте вверх по небольшой дуге, не отрывая таз и не стуча гантелями наверху.",
        "• Lie on the bench, shoulder blades squeezed, feet planted, dumbbells over the chest.\n• Lower to the lower chest, elbows about 45–60° from the torso.\n• Press up in a slight arc without lifting the hips or clanking the dumbbells.",
    ),
    "Отжимания в жилете" to Pair(
        "• Наденьте жилет, плотно затяните; руки чуть шире плеч, тело — прямая линия.\n• Опускайтесь до касания грудью пола или почти, локти около 45° к корпусу.\n• Выжимайте вверх без прогиба в пояснице; если форма ломается — снимите жилет.",
        "• Wear the vest snug; hands slightly wider than shoulders, body straight.\n• Lower until the chest nearly touches the floor, elbows about 45° from the torso.\n• Press up without sagging; if the form breaks, take the vest off.",
    ),
    "Сведение рук с резиной перед грудью" to Pair(
        "• Закрепите резину за спиной на уровне груди, руки разведены, локти слегка согнуты.\n• Сводите руки перед грудью по дуге, сжимая грудные в конце.\n• Медленно разводите до лёгкого растяжения груди, корпус неподвижен.",
        "• Anchor the band behind you at chest height, arms open, elbows slightly bent.\n• Bring the hands together in front of the chest in an arc, squeezing the pecs.\n• Open slowly to a light chest stretch, torso still.",
    ),
    "Подъём гантелей перед собой" to Pair(
        "• Стоя, гантели перед бёдрами, локти чуть согнуты, корпус прямой.\n• Поднимайте руки перед собой до уровня плеч, не раскачиваясь.\n• Опускайте медленно; не поднимайте выше плеч и не помогайте корпусом.",
        "• Stand, dumbbells in front of the thighs, elbows slightly bent, torso upright.\n• Raise the arms in front to shoulder height without swinging.\n• Lower slowly; don't go above the shoulders or use the torso.",
    ),
    "Жим гантелей сидя" to Pair(
        "• Сидя со спинкой, гантели у плеч, ладони вперёд, пресс напряжён.\n• Выжимайте гантели вверх до почти прямых рук, сводя их над головой.\n• Опускайте до уровня ушей или чуть ниже, поясницу не прогибайте.",
        "• Seated with back support, dumbbells at the shoulders, palms forward, abs braced.\n• Press up to almost straight arms, bringing the dumbbells together overhead.\n• Lower to ear level or slightly below without arching the lower back.",
    ),
    "Жим резины вверх стоя" to Pair(
        "• Встаньте на резину, концы у плеч, ладони вперёд, ягодицы и пресс напряжены.\n• Выжимайте руки вверх до выпрямления, голова чуть уходит вперёд между рук.\n• Опускайте медленно к плечам, корпус не отклоняйте назад.",
        "• Stand on the band, ends at the shoulders, palms forward, glutes and abs tight.\n• Press overhead to straight arms, head moving slightly forward between the arms.\n• Lower slowly to the shoulders without leaning back.",
    ),
    "Махи гантелями в стороны" to Pair(
        "• Стоя, лёгкий наклон вперёд, гантели у бёдер, локти чуть согнуты.\n• Поднимайте руки через стороны до уровня плеч — ведёт локоть, кисть чуть ниже.\n• Опускайте медленно; без рывков корпусом и без подъёма плеч к ушам.",
        "• Stand with a slight forward lean, dumbbells at the thighs, elbows slightly bent.\n• Raise out to the sides to shoulder height — lead with the elbow, hands slightly lower.\n• Lower slowly; no torso swing, no shrugging.",
    ),
    "Тяга гантелей к подбородку" to Pair(
        "• Стоя, гантели перед бёдрами, хват на ширине плеч или шире.\n• Тяните гантели вверх вдоль корпуса до уровня груди — локти выше кистей.\n• Опускайте медленно; не тяните выше груди, если чувствуете дискомфорт в плечах.",
        "• Stand, dumbbells in front of the thighs, shoulder-width or wider.\n• Pull up along the body to chest height — elbows above the hands.\n• Lower slowly; don't go higher than the chest if the shoulders feel pinched.",
    ),
    "Жим Арнольда" to Pair(
        "• Сидя, гантели перед лицом, ладони к себе, локти внизу.\n• Выжимая вверх, разворачивайте ладони вперёд, локти расходятся в стороны.\n• В обратном порядке опускайте и разворачивайте ладони к себе; поясница без прогиба.",
        "• Seated, dumbbells in front of the face, palms toward you, elbows down.\n• Press up while rotating the palms forward, elbows moving out to the sides.\n• Reverse on the way down, rotating the palms back; keep the lower back neutral.",
    ),
    "Разводка гантелей в наклоне" to Pair(
        "• Наклон вперёд почти до параллели, спина прямая, гантели под грудью, локти чуть согнуты.\n• Разводите руки в стороны до уровня корпуса, думая о задней дельте, а не о лопатках.\n• Опускайте медленно; корпус неподвижен, без рывков.",
        "• Hinge forward almost to parallel, back flat, dumbbells under the chest, elbows slightly bent.\n• Raise the arms out to torso level, thinking of the rear delts rather than the shoulder blades.\n• Lower slowly; torso still, no jerking.",
    ),
    "Растяжка резины перед грудью" to Pair(
        "• Резина в прямых руках перед грудью на уровне плеч, хват на ширине плеч.\n• Разводите руки в стороны до касания резиной груди, сводя лопатки.\n• Медленно возвращайте, не опуская руки и не поднимая плечи.",
        "• Band in straight arms in front of the chest at shoulder height, shoulder-width grip.\n• Pull the hands apart until the band touches the chest, squeezing the shoulder blades.\n• Return slowly without dropping the arms or shrugging.",
    ),
    "Растяжка пружинного эспандера" to Pair(
        "• Эспандер в прямых руках перед грудью, локти чуть согнуты, спина прямая.\n• Растягивайте эспандер в стороны до касания груди, сводя лопатки.\n• Возвращайте медленно, не давая пружинам дёрнуть руки.",
        "• Expander in straight arms in front of the chest, elbows slightly bent, back straight.\n• Pull it apart to the chest, squeezing the shoulder blades.\n• Return slowly — don't let the springs yank the arms.",
    ),
    "Шраги с гантелями" to Pair(
        "• Стоя, гантели в опущенных руках по бокам, спина прямая.\n• Поднимайте плечи прямо вверх к ушам, задержитесь на секунду.\n• Опускайте медленно до полного растяжения; не вращайте плечами и не сгибайте руки.",
        "• Stand with dumbbells at your sides, back straight.\n• Lift the shoulders straight up toward the ears and hold a second.\n• Lower slowly to a full stretch; no rolling the shoulders or bending the arms.",
    ),
    "Шраги с резиной" to Pair(
        "• Встаньте на середину резины, концы в опущенных руках.\n• Поднимайте плечи вверх к ушам, пауза наверху.\n• Медленно опускайте; руки прямые, голова прямо.",
        "• Stand on the middle of the band, ends in hands at your sides.\n• Lift the shoulders toward the ears, pause at the top.\n• Lower slowly; arms straight, head neutral.",
    ),
    "Тяга резины к лицу" to Pair(
        "• Закрепите резину на уровне лица, хват сверху, руки прямые.\n• Тяните концы к лицу, разводя локти в стороны и выше плеч, лопатки сводятся.\n• Медленно выпрямляйте руки, корпус не отклоняйте.",
        "• Anchor the band at face height, overhand grip, arms straight.\n• Pull the ends toward the face, elbows out and above the shoulders, shoulder blades squeezed.\n• Straighten the arms slowly without leaning back.",
    ),
    "Тяга гантели одной рукой в наклоне" to Pair(
        "• Рука и колено на скамье, спина прямая и параллельна полу, гантель в свободной руке.\n• Тяните гантель к поясу, локоть идёт назад вдоль корпуса.\n• Опускайте до растяжения широчайшей, не скручивая корпус.",
        "• Hand and knee on the bench, back flat and parallel to the floor, dumbbell in the free hand.\n• Row the dumbbell to the hip, elbow travelling back along the body.\n• Lower to a lat stretch without twisting the torso.",
    ),
    "Подтягивания в жилете" to Pair(
        "• Наденьте жилет, хват сверху чуть шире плеч, повисните, лопатки опущены.\n• Тянитесь грудью к перекладине до подбородка выше неё.\n• Опускайтесь подконтрольно до прямых рук; если повторы с рывком — снимите жилет.",
        "• Wear the vest, overhand grip slightly wider than shoulders, hang with shoulder blades down.\n• Pull the chest toward the bar until the chin clears it.\n• Lower under control; if reps turn into kipping, take the vest off.",
    ),
    "Тяга резины к поясу сидя" to Pair(
        "• Сидя, ноги вперёд, резина зацеплена за стопы, спина прямая.\n• Тяните концы к поясу, локти вдоль корпуса, лопатки сводятся.\n• Медленно выпрямляйте руки, не наклоняясь вперёд и не скругляя спину.",
        "• Sit with legs forward, band around the feet, back straight.\n• Row the ends to the waist, elbows along the body, shoulder blades squeezed.\n• Straighten the arms slowly without leaning forward or rounding.",
    ),
    "Супермен (лёжа на животе)" to Pair(
        "• Лёжа на животе, руки вытянуты вперёд, взгляд в пол.\n• Одновременно поднимите руки, грудь и ноги, напрягая поясницу и ягодицы, задержитесь на 1–2 секунды.\n• Опускайте медленно; не запрокидывайте голову.",
        "• Lie face down, arms reaching forward, eyes on the floor.\n• Lift arms, chest and legs together, squeezing the lower back and glutes; hold 1–2 seconds.\n• Lower slowly; don't crank the head back.",
    ),
    "Наклоны с резиной (гуд-морнинг)" to Pair(
        "• Встаньте на резину, петля за шеей на плечах, колени слегка согнуты.\n• Наклоняйтесь вперёд с прямой спиной, отводя таз назад, до параллели или растяжения бедра.\n• Выпрямляйтесь усилием ягодиц и поясницы, не скругляя спину.",
        "• Stand on the band with the loop behind the neck on the shoulders, knees slightly bent.\n• Hinge forward with a flat back, pushing the hips back, to parallel or a hamstring stretch.\n• Stand up by driving with the glutes and lower back, without rounding.",
    ),
    "Румынская тяга с гантелями" to Pair(
        "• Стоя, гантели перед бёдрами, колени чуть согнуты и не меняют угол.\n• Отводите таз назад, скользя гантелями вдоль ног до середины голени, спина прямая.\n• Возвращайтесь, толкая таз вперёд; не скругляйте спину и не приседайте.",
        "• Stand, dumbbells in front of the thighs, knees slightly bent and fixed.\n• Push the hips back, sliding the dumbbells down the legs to mid-shin with a flat back.\n• Return by driving the hips forward; don't round the back or squat.",
    ),
    "Сгибания рук с гантелями" to Pair(
        "• Стоя, гантели в опущенных руках, ладони вперёд, локти у корпуса.\n• Сгибайте руки до верхней точки, локти остаются на месте.\n• Опускайте медленно до выпрямления; не раскачивайтесь корпусом.",
        "• Stand, dumbbells at your sides, palms forward, elbows at the body.\n• Curl to the top while the elbows stay put.\n• Lower slowly to straight arms; no swinging.",
    ),
    "Концентрированные сгибания" to Pair(
        "• Сидя, локоть упирается во внутреннюю часть бедра, ладонь вверх.\n• Сгибайте руку к плечу, в верхней точке сожмите бицепс.\n• Опускайте медленно до почти прямой руки, плечо неподвижно.",
        "• Seated, elbow braced against the inner thigh, palm up.\n• Curl toward the shoulder and squeeze the biceps at the top.\n• Lower slowly to almost straight, upper arm still.",
    ),
    "Сгибания рук с резиной" to Pair(
        "• Встаньте на резину, концы в руках, ладони вперёд, локти у корпуса.\n• Сгибайте руки до верхней точки, не отводя локти вперёд.\n• Опускайте медленно — резина не должна выдёргивать руки.",
        "• Stand on the band, ends in hands, palms forward, elbows at the body.\n• Curl to the top without letting the elbows drift forward.\n• Lower slowly — don't let the band yank the arms.",
    ),
    "Французский жим с гантелью лёжа" to Pair(
        "• Лёжа на скамье, гантели (или одна двумя руками) над грудью, плечи вертикально.\n• Сгибайте только локти, опуская гантели к голове или за неё.\n• Разгибайте до прямых рук, плечи неподвижны, локти не разводите.",
        "• Lie on the bench, dumbbells (or one in both hands) over the chest, upper arms vertical.\n• Bend only the elbows, lowering toward or behind the head.\n• Extend to straight arms; upper arms still, elbows not flaring.",
    ),
    "Разгибание руки с гантелью в наклоне" to Pair(
        "• Наклон вперёд с опорой на скамью, плечо рабочей руки прижато к корпусу параллельно полу.\n• Разгибайте руку назад до полного выпрямления, двигается только предплечье.\n• Медленно возвращайте до угла 90° в локте, плечо не опускайте.",
        "• Hinge forward with support on the bench, working upper arm pinned to the body, parallel to the floor.\n• Extend the arm back to full lockout — only the forearm moves.\n• Return slowly to 90° at the elbow without dropping the upper arm.",
    ),
    "Обратные отжимания от скамьи" to Pair(
        "• Руки на краю скамьи за спиной, пальцы вперёд, ноги вперёд, таз у скамьи.\n• Опускайтесь, сгибая локти до угла около 90°, локти идут назад, не в стороны.\n• Выжимайтесь вверх трицепсом, плечи не поднимайте к ушам.",
        "• Hands on the bench edge behind you, fingers forward, legs out, hips close to the bench.\n• Lower by bending the elbows to about 90°, elbows going back, not out.\n• Press up with the triceps without shrugging.",
    ),
    "Сгибания запястий с гантелями" to Pair(
        "• Сидя, предплечья на бёдрах, кисти за коленями, ладони вверх, гантели в руках.\n• Сгибайте запястья вверх, поднимая гантели только кистью.\n• Опускайте медленно до растяжения предплечья, предплечья не отрывайте.",
        "• Seated, forearms on the thighs, wrists past the knees, palms up, dumbbells in hands.\n• Curl the wrists up, moving only the hands.\n• Lower slowly to a forearm stretch without lifting the forearms.",
    ),
    "Сгибания рук обратным хватом" to Pair(
        "• Стоя, гантели хватом сверху (ладони назад), локти у корпуса.\n• Сгибайте руки, сохраняя прямые запястья.\n• Опускайте медленно до прямых рук без раскачки.",
        "• Stand, dumbbells in an overhand grip (palms back), elbows at the body.\n• Curl while keeping the wrists straight.\n• Lower slowly to straight arms without swinging.",
    ),
    "Вис на перекладине" to Pair(
        "• Возьмитесь за перекладину хватом сверху на ширине плеч.\n• Висите на прямых руках, плечи слегка опущены и активны, ноги расслаблены.\n• Держите до отказа хвата, спрыгивайте мягко.",
        "• Grab the bar overhand at shoulder width.\n• Hang on straight arms, shoulders slightly set down and active, legs relaxed.\n• Hold until the grip gives out, drop down softly.",
    ),
    "Подъёмы ног в висе" to Pair(
        "• Вис на перекладине, лопатки опущены, тело не раскачивается.\n• Поднимайте ноги (прямые или согнутые) до параллели или выше, скручивая таз.\n• Опускайте медленно, без маха; раскачку гасите паузой внизу.",
        "• Hang from the bar, shoulder blades down, body still.\n• Raise the legs (straight or bent) to parallel or higher, curling the pelvis up.\n• Lower slowly without swinging; kill the swing with a pause at the bottom.",
    ),
    "Скручивания с гантелью" to Pair(
        "• Лёжа на спине, колени согнуты, гантель на груди.\n• Скручивайтесь, отрывая лопатки от пола и приближая рёбра к тазу.\n• Опускайтесь медленно; поясница прижата к полу, шею не тяните руками.",
        "• Lie on your back, knees bent, dumbbell on the chest.\n• Crunch up, lifting the shoulder blades and bringing the ribs toward the pelvis.\n• Lower slowly; lower back stays down, don't pull the neck.",
    ),
    "Скручивания с резиной на коленях" to Pair(
        "• Закрепите резину сверху, встаньте на колени, концы резины у головы.\n• Скручивайтесь вниз, приближая локти к бёдрам, — работает пресс, а не руки.\n• Медленно выпрямляйтесь, таз неподвижен.",
        "• Anchor the band high, kneel, ends of the band by the head.\n• Crunch down, bringing the elbows toward the thighs — the abs work, not the arms.\n• Rise slowly, hips still.",
    ),
    "Боковая планка" to Pair(
        "• Упор на предплечье, локоть под плечом, ноги вместе, тело — прямая линия.\n• Поднимите таз и держите его на одной линии с корпусом.\n• Не проваливайтесь в плечо и не опускайте таз; повторите на другую сторону.",
        "• On one forearm, elbow under the shoulder, feet together, body straight.\n• Lift the hips and keep them in line with the torso.\n• Don't sink into the shoulder or drop the hips; repeat on the other side.",
    ),
    "Наклоны в сторону с гантелью" to Pair(
        "• Стоя, гантель в одной руке, другая на поясе или за головой.\n• Наклоняйтесь в сторону гантели, затем выпрямляйтесь усилием косых противоположной стороны.\n• Двигайтесь только в боковой плоскости, без наклона вперёд; поменяйте сторону.",
        "• Stand with a dumbbell in one hand, the other hand on the waist or behind the head.\n• Bend toward the dumbbell, then straighten up with the obliques of the opposite side.\n• Move only sideways, no forward lean; switch sides.",
    ),
    "Дровосек с резиной" to Pair(
        "• Закрепите резину сверху сбоку, концы в обеих руках, стойка шире плеч.\n• Тяните по диагонали вниз через корпус к противоположному бедру, разворачивая торс.\n• Медленно возвращайте; движение идёт от корпуса, руки почти прямые.",
        "• Anchor the band high to one side, ends in both hands, wide stance.\n• Pull diagonally down across the body to the opposite hip, rotating the torso.\n• Return slowly; the movement comes from the torso, arms nearly straight.",
    ),
    "Гоблет-присед с гантелью" to Pair(
        "• Гантель вертикально у груди, стопы чуть шире плеч, носки слегка врозь.\n• Приседайте между колен до параллели или ниже, спина прямая, пятки на полу.\n• Вставайте, толкаясь пятками; колени смотрят по носкам.",
        "• Hold the dumbbell vertically at the chest, feet slightly wider than shoulders, toes slightly out.\n• Squat between the knees to parallel or below, back straight, heels down.\n• Stand up driving through the heels; knees track over the toes.",
    ),
    "Выпады с гантелями" to Pair(
        "• Стоя, гантели в опущенных руках, корпус прямой.\n• Шагните вперёд и опуститесь, пока заднее колено почти не коснётся пола, переднее над стопой.\n• Оттолкнитесь передней ногой обратно; чередуйте ноги.",
        "• Stand with dumbbells at your sides, torso upright.\n• Step forward and lower until the back knee almost touches the floor, front knee over the foot.\n• Push back with the front leg; alternate legs.",
    ),
    "Приседания в жилете" to Pair(
        "• Наденьте жилет, стопы на ширине плеч, носки слегка врозь.\n• Приседайте до параллели или ниже, спина прямая, колени по носкам.\n• Вставайте, толкаясь всей стопой, без рывка.",
        "• Wear the vest, feet shoulder-width, toes slightly out.\n• Squat to parallel or below, back straight, knees over the toes.\n• Stand up through the whole foot without bouncing.",
    ),
    "Болгарские выпады с гантелями" to Pair(
        "• Задняя стопа на скамье, передняя нога на шаг впереди, гантели в руках.\n• Опускайтесь вниз до почти касания коленом пола, корпус слегка наклонён вперёд.\n• Поднимайтесь усилием передней ноги; колено смотрит по носку.",
        "• Rear foot on the bench, front leg a step ahead, dumbbells in hands.\n• Lower until the back knee nearly touches the floor, torso slightly forward.\n• Rise with the front leg; knee tracks over the toes.",
    ),
    "Ягодичный мост с упором в скамью" to Pair(
        "• Лопатки на краю скамьи, стопы на полу, гантель на тазу.\n• Поднимайте таз до прямой линии от плеч до колен, сожмите ягодицы.\n• Опускайте медленно; подбородок к груди, поясницу не переразгибайте.",
        "• Shoulder blades on the bench edge, feet on the floor, dumbbell on the hips.\n• Drive the hips up to a straight line from shoulders to knees and squeeze the glutes.\n• Lower slowly; chin tucked, don't overarch the lower back.",
    ),
    "Отведение ноги назад с резиной" to Pair(
        "• Петля резины на щиколотке, опора руками на стену или стоя на четвереньках.\n• Отводите ногу назад-вверх усилием ягодицы, не прогибаясь в пояснице.\n• Медленно возвращайте; корпус неподвижен.",
        "• Band loop around the ankle, hands on a wall or on all fours.\n• Kick the leg back and up with the glute without arching the lower back.\n• Return slowly; torso still.",
    ),
    "Становая тяга на одной ноге с гантелью" to Pair(
        "• Стоя на одной ноге, гантель в противоположной руке, колено опорной чуть согнуто.\n• Наклоняйтесь вперёд с прямой спиной, свободная нога уходит назад на одной линии с корпусом.\n• Возвращайтесь усилием ягодицы и бедра опорной ноги, таз не разворачивайте.",
        "• Stand on one leg, dumbbell in the opposite hand, standing knee slightly bent.\n• Hinge forward with a flat back, the free leg going back in line with the torso.\n• Return with the glute and hamstring of the standing leg without rotating the hips.",
    ),
    "Сгибание ног с резиной лёжа" to Pair(
        "• Лёжа на животе, резина закреплена у стоп и зацеплена за щиколотки.\n• Сгибайте ноги к ягодицам, не отрывая таз от пола.\n• Медленно выпрямляйте, контролируя натяжение.",
        "• Lie face down, band anchored at the feet and looped around the ankles.\n• Curl the heels toward the glutes without lifting the hips.\n• Straighten slowly, controlling the tension.",
    ),
    "Приседания сумо с гантелью" to Pair(
        "• Широкая стойка, носки развёрнуты наружу, гантель вертикально в опущенных руках.\n• Приседайте, разводя колени по носкам, спина прямая.\n• Вставайте, сжимая ягодицы и внутреннюю поверхность бёдер.",
        "• Wide stance, toes turned out, dumbbell hanging vertically in both hands.\n• Squat, pushing the knees out over the toes, back straight.\n• Stand up squeezing the glutes and inner thighs.",
    ),
    "Боковые выпады с гантелью" to Pair(
        "• Широкая стойка, гантель у груди.\n• Перенесите вес на одну ногу и сядьте на неё, отводя таз назад; другая нога прямая.\n• Оттолкнитесь обратно в центр; пятка опорной ноги на полу, чередуйте стороны.",
        "• Wide stance, dumbbell at the chest.\n• Shift onto one leg and sit back on it, the other leg straight.\n• Push back to the centre; working heel stays down, alternate sides.",
    ),
    "Сведение ноги с резиной стоя" to Pair(
        "• Закрепите резину сбоку низко, петля на щиколотке ближней ноги.\n• Сводите ногу к опорной и чуть дальше, усилием внутренней поверхности бедра.\n• Медленно возвращайте; корпус прямой, держитесь за опору.",
        "• Anchor the band low to the side, loop around the near ankle.\n• Sweep the leg toward and slightly past the standing leg using the inner thigh.\n• Return slowly; torso upright, hold on to a support.",
    ),
    "Подъёмы на носки с гантелями" to Pair(
        "• Стоя на краю ступеньки носками, пятки свисают, гантели в руках.\n• Поднимайтесь на носки как можно выше, пауза наверху.\n• Опускайте пятки ниже уровня ступеньки до растяжения икр; колени прямые.",
        "• Stand with the balls of the feet on a step edge, heels hanging, dumbbells in hands.\n• Rise onto the toes as high as possible, pause at the top.\n• Lower the heels below the step to a calf stretch; knees straight.",
    ),
    "Подъёмы на носок на одной ноге в жилете" to Pair(
        "• В жилете стоя на одной ноге на ступеньке, держитесь рукой за опору.\n• Поднимайтесь на носок до максимума, пауза наверху.\n• Медленно опускайте пятку ниже ступеньки; повторите на другую ногу.",
        "• In the vest, stand on one leg on a step, holding a support.\n• Rise onto the toes as high as you can, pause at the top.\n• Lower the heel slowly below the step; repeat on the other leg.",
    ),
    "Разгибание стопы с резиной сидя" to Pair(
        "• Сидя с прямыми ногами, резина зацеплена за переднюю часть стопы, концы в руках.\n• Тяните носок от себя, преодолевая сопротивление резины.\n• Медленно возвращайте стопу к себе до растяжения икры.",
        "• Sit with straight legs, band around the ball of the foot, ends in hands.\n• Point the toes away against the band.\n• Slowly pull the foot back to a calf stretch.",
    ),
)

/** Старые варианты названий упражнений → русское название в [TECHNIQUE]. */
val TECHNIQUE_ALIASES: Map<String, String> = mapOf(
    "Резинка на трицепс из за спины" to "Резинка на трицепс из-за спины",
)

/** Техника по названию упражнения (русскому, английскому или старому варианту) или null. */
fun techniqueFor(name: String, russian: Boolean): String? {
    val key = name.trim()
    val ru = TECHNIQUE_ALIASES[key] ?: key
    val entry = TECHNIQUE[ru]
        ?: TECHNIQUE.entries.firstOrNull { (k, _) -> k.equals(ru, ignoreCase = true) }?.value
        ?: DEFAULT_ALL.firstOrNull { it.nameEn.equals(key, ignoreCase = true) }?.let { TECHNIQUE[it.nameRu] }
        ?: return null
    return if (russian) entry.first else entry.second
}

private val DEFAULT_ALL by lazy { DEFAULT_EXERCISES + EXERCISE_PACK_2 }
