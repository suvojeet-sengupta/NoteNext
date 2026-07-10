# NoteNext Secure Share — पूरा System आसान भाषा में समझो

यह doc बताती है कि हमने note-sharing को कैसे **"temporary + secure"** बनाया — हर concept simple example के साथ। कोई heavy jargon नहीं।

---

## 1. एक लाइन में क्या बनाया

पहले: note share करने पर वो **plaintext** (साफ़ text) में server पर हमेशा के लिए पड़ा रहता था, और लोग live edit कर सकते थे।

अब: note **आपके phone पर ही lock (encrypt)** हो जाता है, server पर सिर्फ़ "बंद ताला वाला डिब्बा" जाता है, वो **कुछ समय बाद अपने आप delete** हो जाता है, और चाहो तो **एक बार पढ़ते ही खत्म** (burn) हो जाता है।

> मतलब: server के पास आपका note होते हुए भी, वो उसे **पढ़ ही नहीं सकता।**

---

## 2. Encryption क्या है (सबसे ज़रूरी concept)

**Encryption = किसी text को एक "chabhi (key)" से ऐसे scramble कर देना कि बिना chabhi के कोई पढ़ न सके।**

Example:
- असली note: `"Meeting 5 baje"`
- Encrypt करने के बाद: `"x9$Lm2#pQ..."` (बकवास दिखता है)
- सही chabhi लगाओ → वापस `"Meeting 5 baje"` मिल जाता है।

बिना सही chabhi के वो `x9$Lm2#pQ...` किसी काम का नहीं — कोई भी उसे पढ़े, कुछ समझ नहीं आएगा।

### End-to-End (E2E) का मतलब
"End-to-End" यानी note **सिर्फ़ दो सिरों पर** साफ़ दिखता है:
1. भेजने वाले का phone (जहाँ lock हुआ)
2. पढ़ने वाले का phone/browser (जहाँ unlock हुआ)

**बीच में जो भी है — server, internet, hacker — सबको सिर्फ़ scrambled डिब्बा दिखता है।** यही E2E की ताकत है।

---

## 3. AES-256-GCM क्या है

यह बस **encryption का method (recipe)** है जो हमने use किया — दुनिया का सबसे भरोसेमंद।

तीन हिस्सों में समझो:
- **AES** = lock लगाने का standard तरीका (banks, WhatsApp सब यही use करते हैं)।
- **256** = chabhi कितनी बड़ी/मज़बूत है (256 bits)। इतनी बड़ी कि सबसे fast computer भी करोड़ों साल लगाकर तोड़ न पाए।
- **GCM** = एक extra check जो बताता है "किसी ने बीच में डिब्बे से छेड़छाड़ तो नहीं की"। अगर कोई एक अक्षर भी बदले, unlock **fail** हो जाएगा (चुपचाप गलत data नहीं मिलेगा)।

App में यह code करता है: [`ShareCrypto.kt`](../app/src/main/java/com/suvojeet/notenext/data/share/ShareCrypto.kt)

---

## 4. Chabhi (key) कहाँ जाती है? — यहीं पूरा जादू है

Note lock करने के लिए app एक **random chabhi** बनाता है। अब सवाल: वो chabhi पढ़ने वाले तक कैसे पहुँचे, **बिना server को दिए?**

Answer: **URL के "fragment" हिस्से में।**

### URL fragment क्या है?
एक link को देखो:

```
https://api-notenext.suvojeetsengupta.in/s/abc123#KEYYAHAN
                                          └─ path ─┘ └fragment┘
```

`#` के बाद वाला हिस्सा = **fragment**।

**सबसे important rule:** जब browser कोई link खोलता है, तो `#` के बाद वाला हिस्सा **server को भेजता ही नहीं**। वो सिर्फ़ आपके browser/phone में रहता है। (यह internet का पुराना, पक्का नियम है।)

तो:
- `/s/abc123` → server को जाता है (ताला वाला डिब्बा माँगने के लिए)
- `#KEYYAHAN` → **कभी server को नहीं जाता**, सिर्फ़ chabhi की तरह पढ़ने वाले के डिवाइस में पहुँचता है

इसलिए chabhi और locked note **कभी एक ही जगह नहीं मिलते** server पर। यही reason है कि server note को पढ़ नहीं सकता।

---

## 5. "Zero-Knowledge Server" का मतलब

Server को आपके note के बारे में **"zero knowledge" (कुछ नहीं पता)।** उसके पास बस:
- Locked डिब्बा (`ciphertext`)
- ताले का serial number type चीज़ (`iv` — नीचे समझाया)
- कब expire होना है (`expiresAt`)

चाहे server का पूरा database चोरी हो जाए, चोर को सिर्फ़ बकवास scrambled text मिलेगा — कोई note नहीं पढ़ पाएगा। यह design सबसे secure माना जाता है (जैसे privacy-focused apps करती हैं)।

Backend model: [`Note.ts`](../../Notenext-backend/src/models/Note.ts)

### IV क्या है (छोटा सा concept)
**IV = "Initialization Vector"** — हर बार encrypt करते समय एक random छोटा number। इससे एक ही note दो बार lock करने पर भी **अलग-अलग scrambled output** आता है। यह pattern छुपाने के लिए ज़रूरी है। यह secret नहीं होता, इसलिए server पर रख सकते हैं।

---

## 6. Temporary कैसे बनाया — TTL / Expiry

**TTL = Time To Live = "कितनी देर ज़िंदा रहेगा"।**

MongoDB (database) में एक खास feature है: किसी field पर **TTL index** लगा दो, तो database खुद-ब-खुद उस time के बाद record **delete** कर देता है — बिना किसी को कहे।

हमने `expiresAt` field पर यह लगाया:
- Share बनाते वक्त user चुनता है: **10 min / 1 hour / 1 day / 7 days**
- उस हिसाब से `expiresAt = अभी + चुना हुआ time`
- वो time आते ही database note खुद मिटा देता है।

**Extra safety:** database का auto-delete ~60 second late हो सकता है, इसलिए जब कोई note माँगता है तब भी हम check करते हैं — expire हो चुका तो **410 Gone** (खत्म) return करते हैं, purana note कभी नहीं दिखाते।

---

## 7. Burn After Reading (एक बार पढ़ो, गायब)

यह option ON करो तो note **पहली बार खुलते ही delete** हो जाता है। जैसे "Mission Impossible" का message जो पढ़ते ही जल जाता है 🔥

Technical: जब कोई पढ़ने आता है, हम database में एक ही step में **"दो और साथ-साथ delete करो" (atomic `findOneAndDelete`)** करते हैं। इससे:
- पहला पढ़ने वाला → note मिल जाता है, और वहीं delete हो जाता है
- दूसरा जो बाद में आया → उसे **410 (already read)** मिलता है

"Atomic" का मतलब: दो लोग एक ही समय खोलें तो भी सिर्फ़ **एक** को मिलेगा, दोनों को नहीं (race-condition safe)।

---

## 8. Delete Token — "सिर्फ़ बनाने वाला ही मिटा सके"

App में कोई login/account नहीं है। तो हम कैसे साबित करें कि "यह note मैंने बनाया था, इसलिए मैं delete कर सकता हूँ"?

Solution — **secret delete-token**:
- Share बनाते वक्त server एक random secret token देता है (सिर्फ़ **एक बार**)।
- Server उस token का पूरा रूप नहीं रखता, सिर्फ़ उसका **hash** (fingerprint) रखता है।
- बाद में unshare करने के लिए app वही token भेजता है → server hash match करके delete करता है।

मतलब: सिर्फ़ shareId (public) जानने से कोई note delete नहीं कर सकता — token चाहिए, जो सिर्फ़ बनाने वाले के phone में है।

> **Hash क्या है?** एक one-way fingerprint। token से hash बन सकता है, पर hash से token वापस नहीं। तो server का data लीक भी हो तो token सुरक्षित।

---

## 9. पूरा Flow — Share बनाने से पढ़ने तक (step-by-step)

### 🔵 Part A: Share बनाना (भेजने वाला)
1. User note पर **Share → Link** दबाता है।
2. Dialog खुलता है: **expiry चुनो + "Burn after reading" toggle** ([`ShareLinkConfigDialog`](../app/src/main/java/com/suvojeet/notenext/ui/components/ShareOptionsDialog.kt))।
3. "Create link" दबाते ही:
   - App एक **random chabhi** बनाता है
   - Note (title + content) को उस chabhi से **encrypt** करता है → locked डिब्बा
   - Server को भेजता है: सिर्फ़ **locked डिब्बा + iv + expiry + burn** (chabhi नहीं!)
4. Server note store करके वापस देता है: **key-less link** + **delete-token** + **expiry time**।
5. App उस link के आगे **`#chabhi`** जोड़ देता है → पूरा link तैयार:
   `https://.../s/abc123#chabhi`
6. User वो link WhatsApp/किसी को भेज देता है।

### 🟢 Part B: Note पढ़ना (पाने वाला)

**अगर app installed है** (Android App Links):
1. Link दबाते ही सीधा **NoteNext app** खुलता है।
2. App link में से **shareId** (path से) और **chabhi** (fragment से) निकालता है ([`MainViewModel.extractShareKey`](../app/src/main/java/com/suvojeet/notenext/ui/MainViewModel.kt))।
3. Server से locked डिब्बा माँगता है → chabhi से **unlock (decrypt)** → note **read-only** दिखता है ([`SharedNoteScreen`](../app/src/main/java/com/suvojeet/notenext/ui/shared/SharedNoteScreen.kt))।

**अगर app नहीं है** (browser):
1. Link browser में खुलता है → server एक **खाली shell page** भेजता है (उसमें note नहीं होता!)।
2. Page का JavaScript `#` के बाद वाली **chabhi** पढ़ता है (जो browser में ही है)।
3. Server से locked डिब्बा `fetch` करता है → browser में **Web Crypto** से decrypt करता है।
4. **DOMPurify** से safe बनाकर note दिखाता है।

Server ने कभी chabhi नहीं देखी, कभी note नहीं पढ़ा। ✅

---

## 10. कौन सी File क्या करती है

### Backend (`Notenext-backend`)
| File | काम |
|---|---|
| [`models/Note.ts`](../../Notenext-backend/src/models/Note.ts) | Database structure: ciphertext, iv, expiresAt (TTL), burnAfterRead, deleteTokenHash |
| [`routes/notes.ts`](../../Notenext-backend/src/routes/notes.ts) | API: share बनाओ / माँगो / delete करो + expiry-burn logic |
| [`server.ts`](../../Notenext-backend/src/server.ts) | Server setup, `/s/:id` page, security headers |
| [`views/sharePage.ts`](../../Notenext-backend/src/views/sharePage.ts) | Browser वाला decrypt-in-browser page |

### App (`NoteNext`)
| File | काम |
|---|---|
| [`ShareCrypto.kt`](../app/src/main/java/com/suvojeet/notenext/data/share/ShareCrypto.kt) | Encrypt / decrypt (AES-256-GCM) |
| [`ShareModels.kt`](../app/src/main/java/com/suvojeet/notenext/data/share/ShareModels.kt) | Data shapes + expiry options |
| [`ShareRepository.kt`](../app/src/main/java/com/suvojeet/notenext/data/share/ShareRepository.kt) | Encrypt → upload, fetch → decrypt |
| [`ShareOptionsDialog.kt`](../app/src/main/java/com/suvojeet/notenext/ui/components/ShareOptionsDialog.kt) | Expiry + burn वाला dialog |
| [`SharedNoteScreen.kt`](../app/src/main/java/com/suvojeet/notenext/ui/shared/SharedNoteScreen.kt) | Note read-only दिखाना |
| [`MainViewModel.kt`](../app/src/main/java/com/suvojeet/notenext/ui/MainViewModel.kt) | Deep link से shareId + chabhi निकालना |

---

## 11. Deep Link + App Links (link दबाते ही app कैसे खुलता है)

- **App Links**: Google को यह proof (`assetlinks.json`, server पर होस्ट) कि "यह website इसी app की है"। इसलिए `https://.../s/...` link सीधा app में खुलता है, browser में नहीं।
- Link की **chabhi (`#` वाला हिस्सा)** app तक `intent.data` के ज़रिए पहुँचती है — Android उसे नहीं काटता।
- Browser के "Open in app" button के लिए chabhi एक **intent extra (`k`)** से भी भेजी जाती है (backup तरीका)।

---

## 12. क्यों यह Design Secure है — Summary

| खतरा | बचाव |
|---|---|
| Server hack / database चोरी | सिर्फ़ scrambled डिब्बा मिलेगा, note नहीं (chabhi server पर है ही नहीं) |
| Note हमेशा पड़ा रहे | TTL से auto-delete (10min–7day) |
| कोई दोबारा पढ़े | Burn-after-read से एक बार में खत्म |
| कोई और note मिटा दे | Delete-token सिर्फ़ बनाने वाले के पास |
| बीच में data बदल दे | GCM check unlock fail कर देगा |
| Browser में XSS | Decrypt के बाद DOMPurify से clean |

---

## ⚠️ ज़रूरी नोट (अभी बाकी है)
यह code अभी **compile/test नहीं हुआ**। Deploy से पहले:
1. **Backend:** `npm install && npm run build` फिर एक share बनाकर test
2. **App:** Gradle build करके एक बार पूरा flow चलाकर देखो (share → link खोलो → expire/burn test)

---

*बना: E2E encrypted, ephemeral, burn-after-read sharing — collaboration हटाकर।*
