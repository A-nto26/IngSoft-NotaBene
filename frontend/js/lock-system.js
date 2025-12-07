// =====================================
// SYSTEM LOCK – modulo indipendente
// =====================================
let lockActive = false;
let lockCountdown = null;

// countdown
let lockSeconds = 180; // 3 minuti
let lockConfirmTimeout = null;

// tracking
let currentLockedNoteId = null;
let currentUser = null;
let currentLockToast = null;

// snapshot iniziale per evitare salvataggi inutili
let initialSnapshot = null;

// =====================================
// INIT
// =====================================
function initLockSystem(user) {
    currentUser = user;
}

// =====================================
// API CALLS
// =====================================
async function apiLock(noteId) {
    const res = await fetch(`${API_NOTES}/${noteId}/lock?user=${encodeURIComponent(currentUser)}`, {
        method: "POST"
    });

    if (res.ok) return { ok: true };

    if (res.status === 409) {
        const data = await res.json();
        showToast("error", `Nota già in modifica da ${data.lockedBy}.`);
        return { ok: false };
    }

    return { ok: false };
}

async function apiUnlock(noteId) {
    try {
        await fetch(`${API_NOTES}/${noteId}/unlock?user=${encodeURIComponent(currentUser)}`, {
            method: "POST"
        });
    } catch (err) {
        console.warn("Unlock fallito:", err);
    }
}

async function apiRefresh(noteId) {
    await fetch(`${API_NOTES}/${noteId}/lock/refresh?user=${encodeURIComponent(currentUser)}`, {
        method: "POST"
    });
}

// =====================================
// START LOCK
// =====================================
async function startLock(noteId) {
    
    const lock = await apiLock(noteId);
    if (!lock.ok) return false;

    initialSnapshot = {
    titolo: document.getElementById("titolo")?.value || "",
    contenuto: document.getElementById("contenuto")?.value || "",
    cartella: document.getElementById("cartella")?.value || "",
    colore: document.getElementById("folderColorInput")?.value || ""
};

    lockActive = true;
    currentLockedNoteId = noteId;

    startCountdown(noteId);

    return true;
}

// =====================================
// COUNTDOWN
// =====================================
function startCountdown(noteId) {
    lockSeconds = 180; 

    clearInterval(lockCountdown);

    lockCountdown = setInterval(() => {
        lockSeconds--;

        if (lockSeconds <= 0) {
            clearInterval(lockCountdown);
            lockCountdown = null;
            askStillEditing(noteId);
        }
    }, 1000);
}

// =====================================
// ASK STILL EDITING
// =====================================
function askStillEditing(noteId) {

    // Chiudi eventuale vecchio toast
    if (currentLockToast) {
        currentLockToast.remove();
        currentLockToast = null;
    }

    currentLockToast = showConfirmToast(
        "⏳ Stai ancora modificando la nota?",
        async () => {
            // utente ha cliccato SI
            if (currentLockToast) {
                currentLockToast.remove();
                currentLockToast = null;
            }

            await apiRefresh(noteId);

            // estendi lock: +5 minuti → 300 secondi
            lockSeconds = 300;
            clearTimeout(lockConfirmTimeout);
            startCountdown(noteId);
        },
        async () => {
            // utente clicca NO
            clearTimeout(lockConfirmTimeout);
            if (currentLockToast) {
                currentLockToast.remove();
                currentLockToast = null;
            }
            await autoSaveAndExit(noteId);
        }
    );

    // Timeout di 2 minuti se l’utente NON risponde
    lockConfirmTimeout = setTimeout(async () => {

        if (currentLockToast) {
            currentLockToast.remove();
            currentLockToast = null;
        }

        // 2. Rimuovi QUALSIASI altro toast di conferma eventualmente rimasto
        document.querySelectorAll(".toast-confirm").forEach(t => t.remove());

        // 3. Procedi con l'auto-save
        await autoSaveAndExit(noteId);

    }, 120000);
}

// =====================================
// AUTOSAVE + EXIT
// =====================================
async function autoSaveAndExit(noteId) {

    const titolo = document.getElementById("titolo")?.value.trim() || "";
    const contenuto = document.getElementById("contenuto")?.value.trim() || "";
    const cartella = document.getElementById("cartella")?.value.trim() || "";
    const coloreCartella = document.getElementById("folderColorInput")?.value || "#ffb347";

    // Controlla se ci sono state modifiche
    const hasChanged =
        titolo !== initialSnapshot.titolo ||
        contenuto !== initialSnapshot.contenuto ||
        cartella !== initialSnapshot.cartella ||
        coloreCartella !== initialSnapshot.colore;

    if (hasChanged) {
        const body = {
            titolo,
            contenuto,
            cartella: cartella || null,
            coloreCartella
        };

        await fetch(`${API_NOTES}/${noteId}?user=${encodeURIComponent(currentUser)}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });

        showToast("info", "💾 Salvataggio automatico effettuato.");
    }

    await apiUnlock(noteId);
    stopLock();

    chiudiModal("noteModal");
    await caricaNote();
}

// =====================================
// STOP LOCK
// =====================================
function stopLock() {
    lockActive = false;
    currentLockedNoteId = null;

    clearInterval(lockCountdown);
    clearTimeout(lockConfirmTimeout);

    lockCountdown = null;
    lockConfirmTimeout = null;

    if (currentLockToast) {
    currentLockToast.remove();
    currentLockToast = null;
    }
}