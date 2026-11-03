# Публикация в каталогах приложений

## IzzyOnDroid (быстро, подпись остаётся нашей)

Репозиторий IzzyOnDroid берёт готовый APK из [Releases](https://github.com/Sonnik73/SonnikToPoer/releases),
поэтому уже установленные копии обновляются без переустановки. Новые версии подхватываются автоматически.

1. Войти на https://gitlab.com (можно через GitHub).
2. Открыть https://gitlab.com/IzzyOnDroid/repo/-/issues/new, выбрать шаблон **Inclusion request**.
3. Вставить текст ниже и отправить.

```
Application name: SonnikToPower
Package name: com.sonnik.topower
Source code: https://github.com/Sonnik73/SonnikToPoer
License: GPL-3.0-only
APK: attached to GitHub Releases (https://github.com/Sonnik73/SonnikToPoer/releases): SonnikToPower-X.Y.apk (please use this one; the -fdroid- APK is for F-Droid reproducible builds)
Metadata: Fastlane structure in the repo (fastlane/metadata/android, en-US and ru-RU)
Category: Sports & Health

Short description: Offline workout log: progress, personal records and muscle recovery.

No ads, no trackers. Works offline; the network is optional — update checks on GitHub and dated backups to Google Drive (sign-in uses Google Play Services: NonFreeDep). A separate "fdroid" flavor without Google libraries and without network access is built for F-Droid.
Interface in Russian and English.
```

## F-Droid (основной каталог, F-Droid собирает сам; воспроизводимая сборка — с нашей подписью)

**Две сборки из одного кода** (`productFlavors` в `app/build.gradle.kts`):

| | `full` — GitHub, IzzyOnDroid | `fdroid` — F-Droid |
|---|---|---|
| Сервисы Google Play (`play-services-auth`) | есть — копии по датам со входом в Google | нет |
| Проверка обновлений на GitHub | есть | нет, обновляет клиент F-Droid |
| Разрешение `INTERNET` | есть | нет |
| Копия файлом на Google Диске через «Сохранить в…» | есть | есть (без входа, через приложение Диска) |

CI собирает обе и проверяет, что в `fdroid` нет классов `com.google.android.gms` и разрешения на сеть
(шаг «Check F-Droid build»). В релизы GitHub идёт `full`.

**Перед подачей:** репозиторий на GitHub должен быть **публичным** — F-Droid собирает из исходников.

**Воспроизводимая сборка.** В каждом релизе лежит и `SonnikToPower-fdroid-X.Y.apk` — сборка `fdroid`,
подписанная нашим ключом. F-Droid собирает её сам из того же коммита, сверяет побайтно и публикует
**с нашей подписью** (`Binaries` и `AllowedAPKSigningKeys` в метаданных). Поэтому версии из F-Droid и с GitHub
ставятся друг поверх друга, а обмен данными с «Крепостью» (разрешение уровня `signature`) работает с любой.
CI проверяет воспроизводимость на каждой сборке (шаг «Check reproducible F-Droid build»): сборка в другой
папке и без ключа должна совпасть по содержимому. Для этого в `fdroid` отключены профиль ART и сведения о git.


1. Войти на https://gitlab.com, сделать форк https://gitlab.com/fdroid/fdroiddata.
2. В форке создать ветку `com.sonnik.topower` и файл `metadata/com.sonnik.topower.yml`
   с содержимым [com.sonnik.topower.yml](com.sonnik.topower.yml).
3. Открыть merge request в `fdroid/fdroiddata` (шаблон «App inclusion») и отметить пункты чек-листа:
   приложение открытое (GPL-3.0); сборка `fdroid` — без проприетарных библиотек, без сети, трекеров и рекламы.
4. Ответить на вопросы рецензентов. После принятия F-Droid соберёт сборку `fdroid` с тега `v1.33`,
   а новые теги `vX.Y` будет находить сам.

## При каждом выпуске

- `gradle.properties`: увеличить `appVersion` и `appVersionCode` (major × 100 + minor: 1.21 → 121).
- Раздел в `changelog/changelog_ru.md` и `changelog_en.md`.
- `fastlane/metadata/android/{ru-RU,en-US}/changelogs/<appVersionCode>.txt` — те же пункты (до 500 символов).

Всё это проверяет тест `ChangelogTest`.
