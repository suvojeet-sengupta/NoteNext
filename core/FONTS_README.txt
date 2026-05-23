NoteNext — Editorial "Ink & Paper" bundled fonts
=================================================

These three OFL-licensed (SIL Open Font License) families are bundled as
static .ttf instances in core/src/main/res/font/. All are free to
redistribute under the OFL.

FRAUNCES  — https://fonts.google.com/specimen/Fraunces
  source: github.com/undercasetype/Fraunces (fonts/ttf, 72pt optical size)
    fraunces_light.ttf          (Fraunces72pt-Light)
    fraunces_light_italic.ttf   (Fraunces72pt-LightItalic)
    fraunces_regular.ttf        (Fraunces72pt-Regular)
    fraunces_italic.ttf         (Fraunces72pt-Italic)
    fraunces_semibold.ttf       (Fraunces72pt-SemiBold)
    fraunces_semibold_italic.ttf(Fraunces72pt-SemiBoldItalic)
  Note: Fraunces has no static Medium instance; FontWeight.Medium maps to
  SemiBold in Fonts.kt.

DM SANS   — https://fonts.google.com/specimen/DM+Sans
  source: github.com/googlefonts/dm-fonts (Sans/fonts/ttf)
    dmsans_regular.ttf  dmsans_medium.ttf  dmsans_semibold.ttf  dmsans_bold.ttf

JETBRAINS MONO — https://fonts.google.com/specimen/JetBrains+Mono
  source: github.com/JetBrains/JetBrainsMono (fonts/ttf)
    jetbrainsmono_regular.ttf  jetbrainsmono_medium.ttf

Family wiring lives in core/.../ui/theme/Fonts.kt.
