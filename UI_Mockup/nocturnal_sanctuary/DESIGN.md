---
name: Nocturnal Sanctuary
colors:
  surface: '#111319'
  surface-dim: '#111319'
  surface-bright: '#37393f'
  surface-container-lowest: '#0c0e13'
  surface-container-low: '#191c21'
  surface-container: '#1d2025'
  surface-container-high: '#282a30'
  surface-container-highest: '#33353b'
  on-surface: '#e2e2ea'
  on-surface-variant: '#c7c4d7'
  inverse-surface: '#e2e2ea'
  inverse-on-surface: '#2e3036'
  outline: '#908fa0'
  outline-variant: '#464554'
  surface-tint: '#c0c1ff'
  primary: '#c0c1ff'
  on-primary: '#1000a9'
  primary-container: '#8083ff'
  on-primary-container: '#0d0096'
  inverse-primary: '#494bd6'
  secondary: '#ffb95f'
  on-secondary: '#472a00'
  secondary-container: '#ee9800'
  on-secondary-container: '#5b3800'
  tertiary: '#bfc6dd'
  on-tertiary: '#293042'
  tertiary-container: '#8990a6'
  on-tertiary-container: '#22293b'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e1e0ff'
  primary-fixed-dim: '#c0c1ff'
  on-primary-fixed: '#07006c'
  on-primary-fixed-variant: '#2f2ebe'
  secondary-fixed: '#ffddb8'
  secondary-fixed-dim: '#ffb95f'
  on-secondary-fixed: '#2a1700'
  on-secondary-fixed-variant: '#653e00'
  tertiary-fixed: '#dbe2fa'
  tertiary-fixed-dim: '#bfc6dd'
  on-tertiary-fixed: '#141b2c'
  on-tertiary-fixed-variant: '#3f4759'
  background: '#111319'
  on-background: '#e2e2ea'
  surface-variant: '#33353b'
typography:
  display-lg:
    fontFamily: Geist
    fontSize: 48px
    fontWeight: '300'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Geist
    fontSize: 36px
    fontWeight: '300'
    lineHeight: 44px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Geist
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Geist
    fontSize: 26px
    fontWeight: '400'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Geist
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
    letterSpacing: -0.005em
  title-lg:
    fontFamily: Geist
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: 0.01em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0.01em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Geist
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: Geist
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system is engineered specifically for sleep-adjacent consciousness: the groggy moments between waking and resting, late-night journal recording, and middle-of-the-night alarm interactions. The core personality is tranquil, monastic, intimate, and hyper-legible under low ambient light. 

The aesthetic marries **Atmospheric OLED Minimalism** with tactile, low-stimulus ergonomics. It avoids aggressive high-contrast shifts and loud neon flares in favor of deep oceanic blacks, lunar dust overlays, and warm candle-like glowing accents. Visual elements feel like carved obsidian instruments—grounded, soft to touch, and free of visual jitter. Every micro-interaction avoids sharp flashes, relying instead on lingering, gradual luminous transitions that preserve dark-adapted vision and respect circadian rhythms.

## Colors

The palette operates under strict circadian photobiology principles: absolute OLED black anchors the viewport to mitigate backlight glare, complemented by desaturated indigo and warm lunar amber for low-photopic stress.

- **Background & Base**:
  - `Canvas / Root`: `#07090E` (True nocturnal ink, saves battery and prevents retinal fatigue).
  - `Surface 1 (Base Container)`: `#0D111A` (Deep slate obsidian).
  - `Surface 2 (Elevated Card)`: `#131824` (Subtle lifted twilight).
  - `Surface 3 (Interactive / Hover)`: `#1D2436`.

- **Accents & States**:
  - `Primary (Twilight Indigo)`: `#6366F1` (Used for primary navigation hooks, active states, and dream categorization).
  - `Primary Muted`: `#4338CA` (Low-contrast buttons and unselected chips).
  - `Secondary (Moon Amber Glow)`: `#F59E0B` (Reserved for alarms, waking cues, lucid triggers, and active toggles).
  - `Secondary Glow Tone`: `#B45309` (Warm dim accenting).
  - `Tertiary (Starlight)`: `#E0E7FF` (Subdued high-readability highlight).

- **Typography & Boundaries**:
  - `Text Primary`: `#F1F5F9` (Soft pearl; intentionally off-pure-white to prevent stark contrast glare).
  - `Text Muted`: `#94A3B8` (Slate tint for timestamps and metadata).
  - `Text Subtle`: `#475569` (Inactive indicators and secondary borders).
  - `Border Subtle`: `rgba(255, 255, 255, 0.06)` (Ultra-fine edge definition).

## Typography

Typography balances the structural precision of `Geist` for headers, figures, and metadata labels with the fluid readability of `Inter` for uninterrupted, stream-of-consciousness dream logging. 

- **Display & Headings**: Set in light and regular weights with generous vertical rhythm. Numeric values (such as alarm clocks and lucid dream percentages) utilize tabular figures to prevent horizontal shifts.
- **Body & Journal Prose**: Line-heights for body content are broadened to `1.6x` (`26px` on a `16px` base) to allow tired, dilated eyes to scan paragraphs effortlessly without line confusion.
- **Labels & Micro-data**: Rendered with subtle letter spacing (+0.04em to +0.06em) and uppercase treatments to maintain sharp legibility even at dim screen brightness levels.

## Layout & Spacing

The layout embraces a tactile, thumb-driven fluid hierarchy optimized for single-handed bedside usage.

- **Grid & Alignment**: Standard 4-column layout on mobile devices reflowing to an 8-column layout on foldables/tablets, and a fixed 12-column max-width (768px reading column) for desktop review dashboards.
- **Rhythm & Touch Targets**: Spacing increments are rooted in an 8pt modular system with strict minimum touch targets of 48×48px for all nighttime controls to prevent mis-taps when half-awake.
- **Edge Containment**: Section edges remain anchored away from curved device bezels using `margin` (`1.25rem` / `20px`), ensuring content does not vignette against peripheral darkness.

## Elevation & Depth

Standard shadows are strictly prohibited, as traditional light-source drop shadows break OLED power-saving efficiency and create muddy haloing against pitch-black backgrounds.

Instead, visual hierarchy is constructed through **Tonal Stacking and Micro-Borders**:
- **Level 0 (Void)**: `#07090E` (The app canvas, zero elevation).
- **Level 1 (Substratum)**: `#0D111A` with a 1px border `rgba(255, 255, 255, 0.04)` (Journal lists, timeline tracks).
- **Level 2 (Float / Sheet)**: `#131824` with a 1px border `rgba(224, 231, 255, 0.08)` and an ultra-diffused atmospheric glow: `box-shadow: 0 0 32px rgba(99, 102, 241, 0.05)` (Active modals, wake timers, bottom interaction sheets).
- **Active / Accent Glow**: Interactive toggles and active alarm states cast an amber atmospheric wash using `box-shadow: 0 0 24px rgba(245, 158, 11, 0.12)`.

## Shapes

Shapes utilize balanced, organic curvature (`roundedness: 2`) designed to soften the visual perimeter and mimic rounded river stones or polished obsidian glass.

- **Standard Components**: Buttons, input boxes, and list cells feature `0.5rem` (`8px`) radii.
- **Containers & Sheets**: Journal entries, dashboard widgets, and modal frames use `1rem` (`16px`) radii (`rounded-lg`).
- **Interactive Floating Elements**: Floating action buttons, alarm sliders, and voice recorder toggles use `1.5rem` (`24px`) radii (`rounded-xl`) or fully circular pills to indicate direct tactile affordance.

## Components

### Buttons & Quick Actions
- **Primary Action (Wake/Log Dream)**: Background in muted slate indigo (`#6366F1`), text in `#F1F5F9`, 0.5rem corner radius. Pressed state eases into `#4F46E5` without abrupt scale shifts.
- **Secondary Action (Cancel/Snooze)**: Ghost treatment with a 1px border of `rgba(255, 255, 255, 0.08)`, solid `#0D111A` background, and text in `#94A3B8`.
- **Sensory Safe Mode**: Haptic feedback paired with gradual luminous border illumination rather than screen-brightening color transitions.

### Alarm & Slider Controls
- **Nighttime Alarm Toggle**: Large thumb track (56px minimum height) filled with `#0D111A`. When engaged, the thumb shifts from deep indigo to warm moon amber (`#F59E0B`), emitting a gentle 16px radius ambient bleed.
- **Bedtime Sliders**: Smooth continuous tracks with an integrated volume/brightness curve displayed as a soft dot constellation.

### Chips & Dream Tags
- Pill-shaped tags (`rounded-full`) with a height of 28px, background `#0D111A`, and border `rgba(255, 255, 255, 0.06)`.
- Lucidity indicators (e.g., *Lucid*, *Vivid*, *Nightmare*, *False Awakening*) incorporate a 6px status bead colored either in dream amber (`#F59E0B`) or muted twilight (`#6366F1`).

### Input Fields & Markdown Editor
- Input background matches `#0A0D14` with a subtle inset feel. The border rests at `rgba(255, 255, 255, 0.06)` and eases to a soft `rgba(99, 102, 241, 0.4)` upon focus.
- Markdown headers, blockquotes, and lists render with subdued hierarchy: headers appear in soft pearl (`#F1F5F9`), while blockquotes feature a left border in amber (`#F59E0B`) with an opacity of 40%.

### Cards & Audio Journal Logs
- **Dream Entry Card**: Outer boundary outlined with 1px `rgba(255, 255, 255, 0.04)`. Header displays the sleep cycle timestamp in `label-sm` slate text, followed by the dream title in `title-lg`.
- **Audio Waveform Node**: Voice entries display audio amplitudes as low-intensity vertical bars in `#475569`, illuminating to `#F59E0B` during scrub or playback.

### Status Indicators
- Micro status dots (3–6px) for syncing, battery-friendly sleep tracking, and alarm readiness, using low-luminescence amber and indigo to communicate state without piercing the dark.