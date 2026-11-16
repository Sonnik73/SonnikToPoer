# Правила R8 для SonnikToPower.
# Room, Compose и kotlinx-serialization приносят свои consumer-правила в библиотеках —
# здесь остаётся то, что не покрывается ими.

# Стектрейсы читаемыми: имена хотя бы в логах сборки.
-keepattributes SourceFile,LineNumberTable
