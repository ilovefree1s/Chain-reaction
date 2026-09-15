# Retired cards

Cards taken out of the game, kept here whole so any of them can be put back
exactly as they were. Card ids are never reused, so a retired card's id stays
free for it.

## How to bring a card back

1. Paste its **Spec entry** back into the `cards` array in `BUILD_SPEC.md`, in id
   order, and take its id out of `retired` (the build refuses a card that is
   both). If it had `wheelExcludes` or `wheelOnly` membership, add the id back
   there too.
2. Paste its **Android entry** back into `ALL` in
   `app/src/main/java/com/chainreaction/data/GameCard.kt`, in id order (and into
   `WHEEL_EXCLUDES` / `WHEEL_ONLY` if listed below).
3. Put its **Wheel wording** back into `WHEEL_TEXT` in `web/template.html`, and
   the id back into any **Wheel lists** named below (`IGNORES_EXEMPT`,
   `SHORT_STRAW`, `SAYS_IT_ALL`, `ONLY_EXEMPT`, `TEAM_CARDS`).
4. `node web/build.js`, then check the card in the library and on the wheel.

---

## 33 · Lefty ON the Box

Retired 2026-09-15 (v1.3.86). Was: common, dual, Before tee shot, on the wheel.

**Spec entry** (`BUILD_SPEC.md`, `cards`):

```json
    { "id": 33, "timing": "Before tee shot", "kind": "dual", "name": "Lefty ON the Box", "text": "Your first throw off the box is with your offhand, for free — that's throw 0. The player in last place gets it too. If 2 players are tied for last, they can flip a coin or roll dice to see who gets it." },
```

No `rarity` field: it drew as common by default. Not in `wheelExcludes` or
`wheelOnly`.

**Android entry** (`GameCard.kt`, `ALL`):

```kotlin
        GameCard(33, "Before tee shot", CardKind.DUAL, "Lefty ON the Box", "Your first throw off the box is with your offhand, for free — that's throw 0. The player in last place gets it too. If 2 players are tied for last, they can flip a coin or roll dice to see who gets it."),
```

**Wheel wording** (`web/template.html`, `WHEEL_TEXT`):

```js
  33: "[exempt] throws offhand off the box for free — that's throw 0. Everyone else throws lefty too, and theirs counts as throw 1.",
```

**Wheel lists** (`web/template.html`):

```js
// in IGNORES_EXEMPT
  33,  // Lefty ON the Box — the name's offhand throw is free, everyone else's counts
```
