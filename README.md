# בית בחלל – מסך בית לטלוויזיה

Launcher לאנדרואיד TV / Google TV: רובוט מנופף, כוכבים, טיל, ו-4 אריחים (YouTube, סלקום TV, Netflix, HDMI) עם ניווט בחיצי השלט.

## מבנה

- `app/src/main/java/.../MainActivity.java` – כל המסך: בניית הסצנה, האנימציות, הפוקוס והפתיחה של האפליקציות.
- `app/src/main/res/drawable/` – הציורים (כוכב הלכת, הטיל, הרובוט, האייקונים) כ-Vector Drawables.
- `app/src/main/res/font/` – הפונטים Rubik (עברית) ו-Fredoka (השעון).

המסך בנוי על "במה" בגודל 1280×720, בדיוק כמו המוקאפ, והיא מוגדלת לגודל המסך האמיתי.

## התקנה על הטלוויזיה

1. **לפתוח את הפרויקט**: לחלץ את ה-ZIP ולפתוח את התיקייה `SpaceHome` ב-Android Studio (File › Open). לחכות לסנכרון של Gradle.
2. **להפעיל ניפוי באגים בטלוויזיה**: הגדרות › מערכת › מידע › ללחוץ 7 פעמים על Build. אחר כך ב"אפשרויות מפתחים" להדליק **USB debugging** (ואם יש, גם **Network debugging**).
3. **למצוא את כתובת ה-IP של הטלוויזיה**: הגדרות › רשת ואינטרנט › הרשת המחוברת.
4. **להתחבר מהמחשב** (באותה רשת WiFi), בטרמינל של Android Studio:
   ```
   adb connect <IP-של-הטלוויזיה>:5555
   ```
   על הטלוויזיה תופיע בקשת אישור – לאשר.
5. **להריץ**: לבחור את הטלוויזיה ברשימת המכשירים וללחוץ ▶ Run. האפליקציה תיפתח.

## להפוך אותו למסך הבית

ב-Google TV כפתור ה-Home פותח תמיד את מסך הבית של Google. **אל תכבי את המסך של Google** (`pm disable-user`): בלעדיו הטלוויזיה נשארת עם מסך שחור.

במקום זה, באפליקציה יש שירות נגישות (`HomeWatcherService`) שמזהה מתי המסך של Google נפתח ופותח מיד את מסך החלל. מפעילים אותו פעם אחת:
```
adb shell settings put secure enabled_accessibility_services com.dudai.spacehome/com.dudai.spacehome.HomeWatcherService
adb shell settings put secure accessibility_enabled 1
```
כדי לבטל:
```
adb shell settings delete secure enabled_accessibility_services
```

## הגדרות שאפשר לשנות (בראש MainActivity.java)

- `HDMI_INDEX` – איזו כניסת HDMI האריח פותח (0 = הראשונה).
- `apps` – שמות, צבעים ושמות החבילות של האפליקציות. סלקום TV נמצאת לפי השם שלה, אז אין צורך לדעת את שם החבילה.
