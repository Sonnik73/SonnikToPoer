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
APK: attached to GitHub Releases (https://github.com/Sonnik73/SonnikToPoer/releases), one universal APK per release
Metadata: Fastlane structure in the repo (fastlane/metadata/android, en-US and ru-RU)
Category: Sports & Health

Short description: Offline workout log: progress, personal records and muscle recovery.

No ads, no trackers. Works offline; the network is optional — update checks on GitHub and backups to Google Drive (Drive sign-in uses Google Play Services, which F-Droid flags as a non-free dependency).
Interface in Russian and English.
```

## F-Droid (основной каталог, F-Droid собирает сам и подписывает своим ключом)

Установленные из GitHub/IzzyOnDroid копии **не обновятся** версией из F-Droid (другая подпись):
перед переходом сделайте резервную копию (Настройки → Резервная копия), удалите приложение и поставьте из F-Droid.

1. Войти на https://gitlab.com, сделать форк https://gitlab.com/fdroid/fdroiddata.
2. В форке создать ветку `com.sonnik.topower` и файл `metadata/com.sonnik.topower.yml`
   с содержимым [com.sonnik.topower.yml](com.sonnik.topower.yml).
3. Открыть merge request в `fdroid/fdroiddata` (шаблон «App inclusion») и отметить пункты чек-листа:
   приложение открытое (GPL-3.0), без проприетарных библиотек, без трекеров и рекламы.
4. Ответить на вопросы рецензентов. После принятия F-Droid соберёт версию с тега `v1.20`,
   а новые теги `vX.Y` будет находить сам.

## При каждом выпуске

- `gradle.properties`: увеличить `appVersion` и `appVersionCode` (major × 100 + minor: 1.21 → 121).
- Раздел в `changelog/changelog_ru.md` и `changelog_en.md`.
- `fastlane/metadata/android/{ru-RU,en-US}/changelogs/<appVersionCode>.txt` — те же пункты (до 500 символов).

Всё это проверяет тест `ChangelogTest`.
