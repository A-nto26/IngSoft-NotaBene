// =====================================================
// CONFIGURAZIONE BASE
// =====================================================
const API_NOTES = "http://localhost:8080/api/notes";
const API_USERS = "http://localhost:8080/api/users";

const noteList = document.getElementById("noteList");
const welcomeUser = document.getElementById("welcomeUser");
const user = localStorage.getItem("loggedUser");
function normUser(u) {
  return (u || "").trim().toUpperCase();
}
// Stato per tracciare la storia dell'utente
let hasEverCreatedNote = localStorage.getItem(`hasEverCreatedNote_${user}`) === "true"; 

// chiama lock system
initLockSystem(user);

// Stato globale
let editingNoteId = null;
let utentiSelezionati = [];
let showMie = true;
let showCondivise = true;
let modalInitialized = false;
let folderColors = {};
window._noteVersions = {};          
window._versionLoadedAtOpen = {};

// =====================================================
// NORMALIZZAZIONE PERMESSO (NoteView compatibile)
// =====================================================
function normalizzaPermesso(p) {
  if (!p) return "privata";

  if (typeof p === "string") return p.toLowerCase();

  if (typeof p === "object" && p.tipo) return p.tipo.toLowerCase();

  return "privata";
}


function loadFolderColors() {
    folderColors = {};
    Object.keys(localStorage).forEach(key => {
        if (key.startsWith("folderColor_")) {
            const name = key.replace("folderColor_", "");
            folderColors[name] = localStorage.getItem(key);
        }
    });
}

loadFolderColors();


/*  COLORE CARTELLA  */
window.folderPickr = null;
// Flag per permettere o meno l'inizializzazione del color picker (utile per anteprime)
window.allowFolderPickr = true;
// Observer usato per proteggere temporaneamente lo stile del pulsante colore in anteprima
// window.__folderColorObserver removed (diagnostic observer cleaned up)

function resetPickr() {
  if (window.folderPickr) {
    try { 
      window.folderPickr.destroyAndRemove(); 
    } catch (e) {}
    window.folderPickr = null;
  }
}

// Inizializza Pickr
function initFolderColorPicker(defaultColor = "#ffb347") {
  // Se l'inizializzazione è temporaneamente disabilitata (es. anteprima)
  if (!window.allowFolderPickr) return;

  resetPickr();

  const btn = document.getElementById("folderColorBtn");
  const input = document.getElementById("folderColorInput");

  if (!btn || !input) return;

  window.folderPickr = Pickr.create({
    el: btn,
    theme: "classic",
    default: defaultColor,
    position: "bottom-middle",
    components: {
      preview: true,
      opacity: false,
      hue: true,
      interaction: { hex: true, input: true, save: true }
    }
  });

  window.folderPickr.on("change", (color) => {
    const hex = color.toHEXA().toString();
    btn.style.backgroundColor = hex;
    input.value = hex;
  });

  window.folderPickr.on("save", () => window.folderPickr.hide());
}

// =====================================================
// NUOVO SISTEMA TOAST (popup in alto a destra)
// =====================================================
function showToast(type, message) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;

  // contenuto testuale (l’icona arriva dal CSS ::before)
  const text = document.createElement("span");
  text.textContent = message;

  toast.appendChild(text);
  container.appendChild(toast);

  // auto-remove dopo 5s
  setTimeout(() => {
    toast.style.opacity = "0";
    setTimeout(() => toast.remove(), 400);
  }, 5000);
}


// =====================================================
// CONFIRM TOAST (popup grafico Sì / No)
// =====================================================
function showConfirmToast(message, onConfirm, onCancel = null) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = "toast toast-confirm";

  // Messaggio
  const msg = document.createElement("div");
  msg.textContent = message;
  msg.style.textAlign = "center";

  // Bottoni
  const btnRow = document.createElement("div");
  btnRow.style.display = "flex";
  btnRow.style.gap = "10px";
  btnRow.style.marginTop = "8px";

  const yesBtn = document.createElement("button");
  yesBtn.textContent = "Sì";
  yesBtn.onclick = () => {
    toast.remove();
    if (onConfirm) onConfirm();
  };

  const noBtn = document.createElement("button");
  noBtn.textContent = "No";
  noBtn.onclick = () => {
    toast.remove();
    if (onCancel) onCancel();
  };

  btnRow.appendChild(yesBtn);
  btnRow.appendChild(noBtn);

  toast.appendChild(msg);
  toast.appendChild(btnRow);

  container.appendChild(toast);
}


// =====================================================
//  BLOCCO ACCESSO DIRETTO
// =====================================================
if (!user) {
  alert("⚠️ Devi effettuare l'accesso per accedere alla dashboard.");
  window.location.replace("auth.html");
  throw new Error("Accesso non autorizzato: nessun utente loggato.");
} else if (welcomeUser) {
  welcomeUser.textContent = `Ciao, ${normUser(user)}! 👋`;
}

// =====================================================
//  LOGOUT
// =====================================================
const logoutBtn = document.getElementById("logoutBtn");
if (logoutBtn) {
  logoutBtn.addEventListener("click", () => {
    localStorage.removeItem("loggedUser");
    window.location.replace("auth.html");
  });
}

// =====================================================
// TOGGLE FILTRO NOTE (mostra mie / condivise)
// =====================================================
const toggleMie = document.getElementById("toggleMie");
const toggleCondivise = document.getElementById("toggleCondivise");

if (toggleMie) {
  toggleMie.addEventListener("change", () => {
    showMie = toggleMie.checked;
    caricaNote();
  });
}

if (toggleCondivise) {
  toggleCondivise.addEventListener("change", () => {
    showCondivise = toggleCondivise.checked;
    caricaNote();
  });
}

async function getAllUserNotes() {
  try {
    const res = await fetch(`${API_NOTES}/visible/${encodeURIComponent(user)}`);
    if (!res.ok) return [];

    const raw = await res.json();

    // Normalizzazione NoteView per evitare undefined/null
    return raw.map(n => ({
      id: n.id,
      titolo: n.titolo || "",
      contenuto: n.contenuto || "",
      cartella: n.cartella || null,
      coloreCartella: n.coloreCartella || "#FFD700",
      permesso: normalizzaPermesso(n.permesso),
      creatore: normUser(n.creatore),
      condivisaCon: Array.isArray(n.condivisaCon)
          ? n.condivisaCon.map(u => normUser(u))
          : [],
      autoreUsername: normUser(n.autoreUsername || n.creatore),
      ruolo: n.ruolo || "lettura",

      lockedBy: normUser(n.lockedBy) || null,
      versione: n.versione || 1,

      createdAt: n.createdAt || null,
      lastModifiedAt: n.lastModifiedAt || null,
      lastModifiedBy: n.lastModifiedBy || null
    }));
  } catch (e) {
    console.error("Errore caricamento note complete:", e);
    return [];
  }
}


// =====================================================
// TOOLS PANEL – Apertura/Chiusura (+ / -)
// =====================================================
const toolsPanel = document.getElementById("toolsPanel");
const toolsContent = document.getElementById("toolsContent");
const toolsToggleBtn = document.getElementById("toolsToggleBtn");
const filterRow = document.getElementById("filterRow");

let toolsOpen = true; // stato iniziale

function aggiornaToolsPanel() {
  if (!toolsPanel || !toolsContent || !toolsToggleBtn) return;

  if (toolsOpen) {
    toolsContent.classList.remove("toolsHidden");
    toolsToggleBtn.textContent = "−";
    toolsToggleBtn.setAttribute("data-tooltip", "Riduci");
  } else {
    toolsContent.classList.add("toolsHidden");
    toolsToggleBtn.textContent = "+";
    toolsToggleBtn.setAttribute("data-tooltip", "Espandi");
  }
}

if (toolsToggleBtn) {
  toolsToggleBtn.addEventListener("click", () => {
    toolsOpen = !toolsOpen;
    aggiornaToolsPanel();
  });
}

// =============================
// RENDER UNIFICATO UTENTI
// =============================
function renderUtentiCondivisi(lista) {
    const container = document.getElementById("utentiSelezionati");
    if (!container) return;

    container.innerHTML = ""; // pulisci ma NON inserire testo normale

    if (!lista || lista.length === 0) {
        container.innerHTML = `<span style="color:#777;">nessuno</span>`;
        return;
    }

    lista.forEach(u => {
        const pill = document.createElement("span");
        pill.className = "user-pill-inline";
        pill.textContent = normUser(u);;
        container.appendChild(pill);
    });
}

// =====================================================
// CARICAMENTO NOTE
// =====================================================
async function caricaNote() {

  //  Loader iniziale
  if (noteList) {
    noteList.innerHTML = `<div class="loading-notes">Caricamento note...</div>`;
  }

  // -----------------------------------------------------
  // Riferimenti UI
  // -----------------------------------------------------
  const welcomeBox = document.getElementById("welcomeEmpty");
  const arrow = document.querySelector(".welcome-arrow");
  const searchArea = document.getElementById("searchArea");
  const side = document.getElementById("tutorialSide");
  const noResults = document.getElementById("noResults");

  const tutorialKey = `tutorialShown_${user}`;
  const tutorialShown = localStorage.getItem(tutorialKey) === "true";


  // -----------------------------------------------------
  // CARICO TUTTE LE NOTE DELL’UTENTE
  // -----------------------------------------------------
const allNotes = await getAllUserNotes();


// -----------------------------------------------------
//  NOTIFICA: nota eliminata dall'autore
// -----------------------------------------------------
const storedSharedNotes = JSON.parse(localStorage.getItem(`sharedNotes_${user}`) || "[]");

// Trova ID ancora presenti ora
const currentIds = allNotes.map(n => n.id);

// Cerca note che PRIMA esistevano e ORA no → eliminate
const removed = storedSharedNotes.filter(id => !currentIds.includes(id));

if (removed.length > 0) {
    removed.forEach(async (id) => {
        try {
            // Recuperiamo l’ultimo titolo noto
            const old = JSON.parse(localStorage.getItem(`noteInfo_${id}`) || "{}");

              const titolo = old.titolo || "una nota";
              const autore = old.creatore || "autore sconosciuto";

              showToast("info", `🗑️ ${autore} ha eliminato la nota "${titolo}".`);
        } catch (e) {}
    });
}


// -----------------------------------------------------
// CASO A: UTENTE COMPLETAMENTE NUOVO (prima volta ever)
// -----------------------------------------------------
if (!hasEverCreatedNote && allNotes.length === 0) {

    // Mostra welcome
    if (noteList) noteList.innerHTML = "";
    if (welcomeBox) welcomeBox.classList.add("show");
    if (arrow) arrow.style.display = "block";

    // Nascondi tools e tutorial
    if (toolsPanel) toolsPanel.style.display = "none";
    if (searchArea) searchArea.style.display = "none";
    if (filterRow) filterRow.style.display = "none";
    if (side) side.style.display = "none";
    document.body.classList.remove("tutorial-mode");

    if (noResults) noResults.style.display = "none";

    return;
}

// -----------------------------------------------------
// CASO B: UTENTE NON NUOVO MA SENZA NOTE (ha cancellato tutto)
// -----------------------------------------------------
if (hasEverCreatedNote && allNotes.length === 0) {

    if (welcomeBox) welcomeBox.classList.remove("show");
    if (arrow) arrow.style.display = "none";

    // Tools SEMPRE visibili
    if (toolsPanel) toolsPanel.style.display = "block";
    if (searchArea) searchArea.style.display = "flex";
    if (filterRow) filterRow.style.display = "flex";

    // Nessun tutorial
    if (side) side.style.display = "none";
    document.body.classList.remove("tutorial-mode");

    // Messaggio dashboard
    if (noteList) {
        noteList.innerHTML = `
            <div class="no-notes-message">
        <span class="icon">📭</span>
        <span>Non ci sono note</span>
    </div>
`;

    }

    if (noResults) noResults.style.display = "none";
    // Impedisci che riappaia la freccia
    if (arrow) arrow.style.display = "none";

    return;
}

// -----------------------------------------------------
// MINI-TUTORIAL: SOLO ALLA PRIMA NOTA PRIVATA DELL’UTENTE
// -----------------------------------------------------

// Mostra sempre i tools appena esistono note (dopo i casi vuoti sopra)
if (toolsPanel) toolsPanel.style.display = "block";
if (searchArea) searchArea.style.display = "flex";
if (filterRow) filterRow.style.display = "flex";

// Note create dall’utente
const myNotes = allNotes.filter(n =>
  (n.creatore || "").toLowerCase() === user.toLowerCase()
);

// Regola tutorial:
// - prima nota dell'autore (privata o condivisa) e mai mostrato prima → mostra
const isFirstOwnNote = myNotes.length === 1;

// Protezione: se l'utente ha più di una nota propria (significa non è la prima volta),
// contrassegna il tutorial come mostrato per evitare di ripeterlo in nuovi browser.
if (myNotes.length > 1) {
    localStorage.setItem(tutorialKey, "true");
}

if (!tutorialShown && isFirstOwnNote) {

    if (toolsPanel) toolsPanel.style.display = "block";
    if (searchArea) searchArea.style.display = "flex";
    if (filterRow) filterRow.style.display = "flex";

    // Mostra mini tutorial
    if (side) side.style.display = "block";
    document.body.classList.add("tutorial-mode");

    // Nascondi welcome e freccia
    if (arrow) arrow.style.display = "none";
    if (welcomeBox) welcomeBox.classList.remove("show");

} else {
    // Nessun tutorial
    if (side) side.style.display = "none";
    document.body.classList.remove("tutorial-mode");
    if (arrow) arrow.style.display = "none";
}

  // -----------------------------------------------------
  // CARICO LE NOTE FILTRATE (mie / condivise)
  // -----------------------------------------------------
  let notes = [...allNotes];

  const userNorm = (user || "").trim().toLowerCase();

  notes = notes.filter(n => {
    const creatoreNorm = (n.creatore || "").trim().toLowerCase();
    const isMyNote = creatoreNorm === userNorm;
    const isShared = !isMyNote;

    return (
      (showMie && isMyNote) ||
      (showCondivise && isShared)
    );
  });


  // Nessuna nota nella VISTA CORRENTE 
  if (notes.length === 0) {
    if (noResults) noResults.style.display = "flex";
    return;
  } else {
    if (noResults) noResults.style.display = "none";
  }

  // -----------------------------------------------------
  // AGGIORNO I LOCK GLOBALI
  // (SOLO ORA, DOPO aver ottenuto le note filtrate!)
  // -----------------------------------------------------
  const lockStates = await Promise.all(
    notes.map(n =>
      fetch(`${API_NOTES}/${n.id}/lock`).then(r => r.json())
    )
  );

  notes.forEach((n, i) => {
    n.lockedBy = lockStates[i]?.lockedBy || null;
  });

  // Memorizza per anteprima / modifica
  window.__noteLocks = {};
  notes.forEach(n => {
    if (n.lockedBy) window.__noteLocks[n.id] = n.lockedBy;
  });

  // -----------------------------------------------------
  // PULIZIA UI e ORDINAMENTO NOTE
  // -----------------------------------------------------
  noteList.innerHTML = "";

  // Chiudi eventuale modale aperta
  const noteModalEl = document.getElementById("noteModal");
  if (noteModalEl) noteModalEl.style.display = "none";

  // Ordina note per ID decrescente
  notes.sort((a, b) => (b.id || 0) - (a.id || 0));

  //  Render card
  notes.forEach((n) => {
    const isAutore =
      (n.creatore || "").trim().toLowerCase() ===
      (user || "").trim().toLowerCase();

    const permessoTipo = normalizzaPermesso(n.permesso);
    const versioneCorrente = n.versione || 1;

    // Registra la versione corrente della nota
    window._noteVersions[n.id] = n.versione;


    let badgePermesso = "🔒 Privata";
    if (permessoTipo.includes("scrittura")) badgePermesso = "🖊️ In scrittura";
    else if (permessoTipo.includes("lettura")) badgePermesso = "👓 In lettura";

    const card = document.createElement("div");
    card.dataset.id = n.id;

    // ===============================
    //  BORDO BASATO SUL PERMESSO
    // ===============================
    let cardClass = "note-card";

    // Scrittura → verde
    if (permessoTipo.includes("scrittura")) {
        cardClass += " shared-write";

    // Lettura → azzurro
    } else if (permessoTipo.includes("lettura")) {
        cardClass += " shared-read";

    // Privata → rosso
    } else {
        cardClass += " private-note";
    }

    card.className = cardClass;
        const cartellaName = n.cartella || "";
    const coloreEffettivo =
        folderColors[cartellaName] ||
        n.coloreCartella ||
        "#ffb347";

      // Badge versione
    const badge = document.createElement("div");
    badge.className = "version-badge";
    badge.textContent = `v${versioneCorrente}`;

    // Recupero ultimo contenuto valido
    let contenutoUltimo = n.contenuto || "(vuoto)";

    // Contenitore del contenuto
    const content = document.createElement("div");
    content.className = "card-content" ;

    content.innerHTML = `
        <h2>${n.titolo}</h2>
        <p>${contenutoUltimo}</p>

        <p>
          <small>
            <svg class="folder-icon" style="color:${coloreEffettivo};">
                <use href="#folder-fill"></use>
                <use href="#folder-stroke"></use>
            </svg>
            ${n.cartella || "—"}
          </small>
        </p>

        <p><small>${badgePermesso}</small></p>

        ${permessoTipo === "scrittura"
        ? (
            n.lockedBy
            ? `<p class="lock-indicator">${n.lockedBy === user ? "🟢 In modifica da te" : "🔴 In modifica da " + n.lockedBy}</p>`
            : `<p class="lock-indicator green">🟢 Libera</p>`
          )
        : ""  // niente lock indicator per private o lettura
}

`;

// Footer bottoni
const footer = document.createElement("div");
footer.className = "card-footer";

footer.innerHTML = `
    <button class="action-btn edit">✏️ Modifica</button>
    <button class="action-btn version">🕓 Versioni</button>
    <button class="action-btn delete">🗑️ Elimina</button>
`;

// Assemblaggio card
card.appendChild(badge);
card.appendChild(content);
card.appendChild(footer);


    const editBtn = card.querySelector(".edit");
    const deleteBtn = card.querySelector(".delete");
    const versionBtn = card.querySelector(".version");

// =============================================
//  SE LA NOTA È BLOCCATA DA UN ALTRO UTENTE
// =============================================
if (n.lockedBy && n.lockedBy !== user) {
    // disabilita modifica
    editBtn.disabled = true;
    editBtn.classList.add("disabled-btn");

    // disabilita versioni
    versionBtn.disabled = true;
    versionBtn.classList.add("disabled-btn");
    

    // card click → anteprima sola lettura
    card.onclick = () => {
        apriModalAnteprima(
            n.id,
            n.titolo,
            n.contenuto,
            n.cartella,
            n.ruolo,
            n.condivisaCon,
            n.permesso,
            n.lastModifiedAt,
            n.lastModifiedBy,
            n.createdAt,
            n.creatore,
            n.coloreCartella,
            n.autoreUsername
        );
    };

    // override click versioni
    versionBtn.onclick = (e) => {
        e.stopPropagation();
        showToast("error", `🔒 Versioni bloccate (${n.lockedBy}).`);
    };
}

    //  BLOCCA SEMPRE la propagazione dei click sui bottoni
card.querySelectorAll("button").forEach(btn => {
    btn.addEventListener("click", (e) => {
        e.stopPropagation();
    });
});


    // Autore
    if (isAutore) {
      editBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        apriModalModifica(
          n.id,
          n.titolo,
          n.contenuto,
          n.cartella,
          "autore",
          [...(n.condivisaCon || [])],
          n.permesso,
          n.lastModifiedAt,
          n.lastModifiedBy,
          n.createdAt,
          n.creatore,
          n.coloreCartella,
          n.autoreUsername
        );
      });

      deleteBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        eliminaNota(n.id);
      });

      // Lettura
    } else if (permessoTipo.includes("lettura")) {
      editBtn.textContent = "👁️ Apri";
      editBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        apriModalAnteprima(
          n.id,
          n.titolo,
          n.contenuto,
          n.cartella,
          "lettura",
          n.condivisaCon,
          n.permesso,
          n.lastModifiedAt,
          n.lastModifiedBy,
          n.createdAt,
          n.creatore,
          n.coloreCartella,
          n.autoreUsername
        );
      });

      deleteBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        rimuovitiDallaNota(n.id);
      });

      // Scrittura
    } else if (permessoTipo.includes("scrittura")) {
      editBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        apriModalModifica(
          n.id,
          n.titolo,
          n.contenuto,
          n.cartella,
          "scrittura",
          n.condivisaCon,
          n.permesso,
          n.lastModifiedAt,
          n.lastModifiedBy,
          n.createdAt,
          n.creatore,
          n.coloreCartella,
          n.autoreUsername
        );
      });

      deleteBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        rimuovitiDallaNota(n.id);
      });
    }

    // Versioni
    versionBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      mostraVersioni(n.id);
    });

    // Clic sulla card anteprima in base al ruolo
    card.addEventListener("click", async (e) => {

    //  Blocca click sui bottoni
    if (e.target.tagName === "BUTTON") return;

    //  Blocca click nella barra azioni
    if (e.target.closest(".actions")) return;

    //  Blocca click sul badge versioni
    if (e.target.classList.contains("version-badge")) return;

    // davvero un click sulla card
    const ruolo = n.ruolo;

    //  Aggiornamento automatico per TUTTI prima dell'apertura
try {
    const freshRes = await fetch(`${API_NOTES}/${n.id}?user=${encodeURIComponent(user)}`);
    if (freshRes.ok) {
        const fresh = await freshRes.json();

        const nuovaVersione = fresh.versione;
        const vecchiaVersione = window._noteVersions[n.id];

          if (nuovaVersione !== vecchiaVersione) {

              showToast("info", `ℹ️ La nota è stata aggiornata da ${fresh.lastModifiedBy}.`);

              // Aggiorna la card in RAM (NoteView)
              n.titolo = fresh.titolo;
              n.contenuto = fresh.contenuto;
              n.cartella = fresh.cartella;
              n.coloreCartella = fresh.coloreCartella;

              n.permesso = fresh.permesso;              
              n.condivisaCon = fresh.condivisaCon;      
              n.ruolo = fresh.ruolo;                    

              n.lastModifiedAt = fresh.lastModifiedAt;
              n.lastModifiedBy = fresh.lastModifiedBy;
              n.creatore = normUser(fresh.creatore);
              n.autoreUsername = normUser(fresh.autoreUsername || fresh.creatore);
              n.versione = fresh.versione;
              n.lockedBy = normUser(fresh.lockedBy);

              window._noteVersions[n.id] = fresh.versione;

              await caricaNote();
              return;
          }
    }
} catch (err) {
    console.warn("Errore aggiornamento automatico:", err);
}
    apriModalAnteprima(
          n.id,
          n.titolo,
          n.contenuto,
          n.cartella,
          ruolo,
          n.condivisaCon,
          n.permesso,
          n.lastModifiedAt,
          n.lastModifiedBy,
          n.createdAt,
          n.creatore,
          n.coloreCartella,
          n.autoreUsername
      );
});

    noteList.appendChild(card);
  });

   // -----------------------------------------------------
  //  Salvataggio stato note condivise correnti
 // -----------------------------------------------------

// Salva solo gli ID delle note condivise attuali
const visibleShared = allNotes
    .filter(n => n.creatore.toLowerCase() !== user.toLowerCase())
    .map(n => n.id);

localStorage.setItem(`sharedNotes_${user}`, JSON.stringify(visibleShared));

// Salva i titoli per i messaggi successivi
allNotes.forEach(n => {
    const autore = n.creatore || n.autoreUsername || "autore sconosciuto";

    localStorage.setItem(`noteInfo_${n.id}`, JSON.stringify({
    titolo: n.titolo,
    creatore: n.creatore
}));
});


}


// =====================================================
//  SE ARRIVA newFolder DALLA PAGINA CARTELLE
// =====================================================
function checkNewFolderParam() {
    const params = new URLSearchParams(window.location.search);
    const newFolder = params.get("newFolder");

    if (newFolder) {
        // apri modale Crea Nota
        apriModalCrea();

        // imposta la cartella
        const cartellaEl = document.getElementById("cartella");
        if (cartellaEl) {
            cartellaEl.value = newFolder;
        }

        // messaggio all'utente
        showToast("info", `📁 Creazione nota nella cartella "${newFolder}"`);

        // rimuove il parametro dall'URL per evitare ri-trigger
        history.replaceState({}, document.title, "dashboard.html");
    }
}

function applyFolderColor(startColor, enablePicker = false) {
    setTimeout(() => {
        const btn = document.getElementById("folderColorBtn");
        const input = document.getElementById("folderColorInput");

        if (!btn || !input) return; // HTML non ancora pronto evita errori

        // Imposta il colore visivamente
        btn.style.backgroundColor = startColor;
        input.value = startColor;

        // Usa classe CSS pulita per lo stato preview (gestita in folder-icons.css)
        btn.classList.add("folder-preview");

    // Imposta la flag globale che abilita l'inizializzazione del picker
    window.allowFolderPickr = !!enablePicker;

    // Rimuovi eventuale Pickr precedente
    resetPickr();

    if (enablePicker) {
      // Abilita la modifica colore
      initFolderColorPicker(startColor);
      btn.classList.remove("folder-preview");
      btn.style.pointerEvents = "auto";
    } else {
      // SOLO VISUALIZZAZIONE (Anteprima)
      btn.classList.add("folder-preview");
      btn.style.pointerEvents = "none";
    }
    }, 80); // Garantisce che il modal sia renderizzato
}

// Helper globale: aggiorna lo stato di lock di una singola card nella dashboard
async function refreshCardLock(noteId) {
  try {
    const res = await fetch(`${API_NOTES}/${noteId}/lock`);
    if (!res.ok) return;
    const data = await res.json();
    const lockedBy = data.lockedBy || null;

    // Mantieni cache globale aggiornata
    window.__noteLocks = window.__noteLocks || {};
    if (lockedBy) window.__noteLocks[noteId] = lockedBy;
    else delete window.__noteLocks[noteId];

    // Aggiorna solo la card relativa
    const card = document.querySelector(`.note-card[data-id="${noteId}"]`);
    if (!card) return;
    const lockEl = card.querySelector('.lock-indicator');
    if (lockedBy) {
      const text = lockedBy === user ? '🟢 In modifica da te' : '🔴 In modifica da ' + lockedBy;
      if (lockEl) {
        lockEl.classList.remove('green');
        lockEl.innerHTML = text;
      } else {
        const p = document.createElement('p');
        p.className = 'lock-indicator';
        p.innerHTML = text;
        const content = card.querySelector('.card-content') || card;
        content.appendChild(p);
      }
    } else {
      if (lockEl) {
        lockEl.classList.add('green');
        lockEl.innerHTML = '🟢 Libera';
      } else {
        const p = document.createElement('p');
        p.className = 'lock-indicator green';
        p.innerHTML = '🟢 Libera';
        const content = card.querySelector('.card-content') || card;
        content.appendChild(p);
      }
    }
  } catch (e) {
    
  }
}


// =====================================================
//  MODAL CREA / MODIFICA
// =====================================================
const modal = document.getElementById("noteModal");
const modalTitle = document.getElementById("modalTitle");
const addNoteBtn = document.getElementById("addNoteBtn");
const cancelNoteBtn = document.getElementById("cancelNoteBtn");
const saveNoteBtn = document.getElementById("saveNoteBtn");
const shareNoteBtn = document.getElementById("shareNoteBtn");
const utentiSelezionatiDiv = document.getElementById("utentiSelezionati");
const permessoSelect = document.getElementById("permesso");
const shareSection = document.getElementById("shareSection");
const modalAutore = document.getElementById("modalAutore");

// =====================================================
// CONTATORE CARATTERI NEL CONTENUTO
// =====================================================
const contenutoInput = document.getElementById("contenuto");
const charCount = document.getElementById("charCount");
const MAX_CHARS = 279;

if (contenutoInput && charCount) {
  contenutoInput.addEventListener("input", () => {
    const len = contenutoInput.value.length;
    charCount.textContent = `${len} / ${MAX_CHARS + 1}`;
    if (len > MAX_CHARS) {
      contenutoInput.value = contenutoInput.value.slice(0, MAX_CHARS);
    } else {
      charCount.style.color = "#999";
    }
  });
}

if (addNoteBtn) {
  addNoteBtn.addEventListener("click", apriModalCrea);
}
if (cancelNoteBtn) {
  cancelNoteBtn.addEventListener("click", () => chiudiModal("noteModal"));
}

if (permessoSelect) {
  permessoSelect.addEventListener("change", () => {

    const valore = permessoSelect.value;

    // Se nota in modifica NON resettare le pillole
    const evitaReset = modalInitialized;

    if (valore === "lettura" || valore === "scrittura") {
        shareSection.style.display = "block";

        // NON resettare le pillole se modal è già iniziata
        if (!evitaReset) {
            utentiSelezionati = [];
            renderUtentiCondivisi([]);
        }
    } else {
        shareSection.style.display = "none";

        if (!evitaReset) {
            utentiSelezionati = [];
            renderUtentiCondivisi([]);
        }
    }
});
}

function resetShareButton() {
    shareNoteBtn.disabled = false;
    shareNoteBtn.style.pointerEvents = "auto";
    shareNoteBtn.style.display = "inline-block";
    shareNoteBtn.classList.remove("disabled-preview");
    shareNoteBtn.onclick = null;  // rimuove blocchi residui
}

function apriModalCrea() {
    modal.removeAttribute("aria-hidden");
    modal.inert = false;
    modalInitialized = true;
    resetShareButton();

    // Reset del bottone utenti
    shareNoteBtn.disabled = false;
    shareNoteBtn.classList.remove("disabled-preview");
    shareNoteBtn.style.display = "inline-block";
    shareNoteBtn.onclick = null;

    editingNoteId = null;
    utentiSelezionati = [];

    document.querySelector("#duplicateBtn")?.remove();
    document.querySelector("#leaveShareBtn")?.remove();   
    document.querySelector(".note-footer")?.remove();

    titolo.value = "";
    contenuto.value = "";
    cartella.value = "";
    permessoSelect.value = "privata";
    permessoSelect.disabled = false;

    // Reset condivisione + pillole vuote
    utentiSelezionati = [];
    renderUtentiCondivisi(utentiSelezionati);

    // Nascondi se privato
    shareSection.style.display = "none";

    // Applica stile unificato al pulsante UTENTI
    shareNoteBtn.className = "share-users-btn";
    shareNoteBtn.innerHTML = "👥 Utenti";

    titolo.disabled = false;
    contenuto.disabled = false;
    cartella.disabled = false;

    modalTitle.textContent = "📝 Crea una nuova nota";
    // Nascondi il campo autore nella creazione
    if (modalAutore) {
        modalAutore.style.display = "none";
        modalAutore.textContent = ""; // pulizia
    }

    saveNoteBtn.style.display = "inline-block";
    saveNoteBtn.textContent = "💾 SALVA";
    cancelNoteBtn.style.display = "inline-block";
    shareNoteBtn.style.display = "inline-block";

    modal.style.display = "flex";

    // RESET COLORE CARTELLA
    const defaultColor = "#FFB347";

    // Variabile globale
    selectedColor = defaultColor;

    //  UI del pallino
    applyFolderColor(defaultColor, true);

    //  Pickr (se inizializzato)
    if (window.folderPickr) {
        window.folderPickr.setColor(defaultColor);
    }
}

async function apriModalAnteprima(
    id,
    titoloVal,
    contenutoVal,
    cartellaVal,
    ruolo = "autore",
    utentiCondivisi = [],
    permessoTipo = "Privata",
    lastModifiedAt = null,
    lastModifiedBy = null,
    createdAt = null,
    creatore = null,
    coloreCartella = null,
    autoreUsername = null
) {
    editingNoteId = id;
    modalInitialized = false;
    const versioneCorrente = window._noteVersions[id];
    window._versionLoadedAtOpen[id] = versioneCorrente;
    const lockedByGlobal = window.__noteLocks?.[id] || null;

    // ----------------------------------------------------
    //  CONTROLLO VERSIONE AGGIORNATA (anche in lettura)
    // ----------------------------------------------------
    try {
        const res = await fetch(`${API_NOTES}/${id}?user=${encodeURIComponent(user)}`);
        if (res.ok) {
            const notaServer = await res.json();
            const versioneServer = notaServer.versione;
            if (versioneServer !== versioneCorrente) {

                showToast("info", "🔄 La nota è stata aggiornata da un altro utente. Contenuto aggiornato.");

                titoloVal = notaServer.titolo;
                contenutoVal = notaServer.contenuto;
                cartellaVal = notaServer.cartella;
            }
        }
    } catch (e) {
        console.warn("Errore controllo versione in anteprima:", e);
    }
    
    // RESET BOTTONE UTENTI (prima di metterlo disabled)
  
    resetShareButton();

    //  DISABILITA IL BOTTONE UTENTI IN ANTEPRIMA
  
    shareNoteBtn.disabled = true;
    shareNoteBtn.classList.add("disabled-preview");

    // Blocca definitivamente il click
    shareNoteBtn.onclick = (e) => {
        e.preventDefault();
        e.stopPropagation();
        return false;
    };

    // 2. DISABILITA PERMESSO
    permessoSelect.disabled = true;

    // 3. RESET SEZIONE UTENTI
    utentiSelezionatiDiv.innerHTML = "";

    // 4. PULIZIA ELEMENTI DA APERTURE PRECEDENTI
    document.querySelector("#duplicateBtn")?.remove();
    document.querySelector("#leaveShareBtn")?.remove();
    document.querySelector(".note-footer")?.remove();

    // 5. POPOLA CAMPi
    titolo.value = titoloVal;
    contenuto.value = contenutoVal;
    cartella.value = cartellaVal || "";

    // DEBUG — verifica cosa arriva alla modale
    //console.log("DEBUG ANTEPRIMA:", {
    //   id,
    //   ruolo,
    //   autoreUsername,
    // utentiCondivisiRicevuti: utentiCondivisi  });


    // 5B. MOSTRA CAMPO AUTORE
    if (modalAutore) {
        // Usa "creatore" come autore principale
        const autoreVisibile = autoreUsername || creatore || "autore sconosciuto";

        modalAutore.style.display = "block";
        const autoreNorm = normUser(autoreVisibile);
        modalAutore.innerHTML = `Autore: <span class="author-pill">${autoreNorm}</span>`;
      }

    const perm = normalizzaPermesso(permessoTipo);

    // =============================================
    // NOTA LOCKATA DA ALTRO
    // =============================================
    if (perm === "scrittura" && lockedByGlobal && lockedByGlobal !== user) {
      // blocca input
      titolo.disabled = true;
      contenuto.disabled = true;
      cartella.disabled = true;

      // blocca colore
      const btn = document.getElementById("folderColorBtn");
      if (btn) {
          btn.style.pointerEvents = "none";
          btn.style.opacity = "0.5";
      }

      // blocca versioni
      const versionBtn = document.getElementById("versionBtn");
      if (versionBtn) {
          versionBtn.disabled = true;
          versionBtn.classList.add("disabled-btn");
      }

      // blocca condivisione
      shareNoteBtn.disabled = true;
      shareNoteBtn.classList.add("disabled-preview");
      shareNoteBtn.onclick = (e) => {
          e.stopPropagation();
          showToast("error", `🔒 Nota bloccata da ${lockedByGlobal}`);
      };
    }

    titolo.disabled = true;
    contenuto.disabled = true;
    cartella.disabled = true;

    // Bottoni
    saveNoteBtn.style.display = "none";
    cancelNoteBtn.style.display = "inline-block";
    modalTitle.textContent = "👁️ Anteprima nota";

    // ----------------------------------------------------
    // GESTIONE AREA CONDIVISIONE
    // ----------------------------------------------------
    const p = normalizzaPermesso(permessoTipo);
    const isPrivata = p === "privata";
    // Mostra sempre il permesso, ma disabilitato
    // Normalizza il permesso e imposta il valore corretto nel select
    if (permessoSelect) {
    const permNorm = normalizzaPermesso(permessoTipo); // → "privata" | "lettura" | "scrittura"

    // Assicura che il valore esista tra le option
    const validValues = ["privata", "lettura", "scrittura"];
    permessoSelect.value = validValues.includes(permNorm) ? permNorm : "privata";

    permessoSelect.disabled = true;
}
    //area condivisone
    utentiSelezionati = [...(utentiCondivisi || [])];
    utentiSelezionatiDiv.innerHTML = "";

    // Se la nota è privata non far vedere nulla
if (isPrivata) {
    shareSection.style.display = "none";
} else {
    shareSection.style.display = "flex";

    // AUTORE vede tutti gli utenti
    if (ruolo === "autore") {

        if (utentiSelezionati.length === 0) {
            utentiSelezionatiDiv.innerHTML =
                "<p style='color:#666; margin-top:6px;'>Nessun utente con cui è stata condivisa.</p>";
        } else {
            utentiSelezionati.forEach(u => {
                const pill = document.createElement("span");
                pill.className = "user-pill-inline";
                pill.textContent = normUser(u);
                utentiSelezionatiDiv.appendChild(pill);
            });
        }

    }
    // UTENTE CONDIVISO vede tutti gli utenti tranne: se stesso, l’autore
    else {

        const utentiDaMostrare = utentiSelezionati.filter(u =>
            u !== user && u !== autoreUsername
        );

        if (utentiDaMostrare.length === 0) {
            utentiSelezionatiDiv.innerHTML =
                "<p style='color:#666; margin-top:6px;'>Non ci sono altri utenti con accesso.</p>";
        } else {
            utentiDaMostrare.forEach(u => {
                const pill = document.createElement("span");
                pill.className = "user-pill-inline";
                pill.textContent = normUser(u);
                utentiSelezionatiDiv.appendChild(pill);
            });
        }
    }
};

    // ----------------------------------------------------
    //  COLORE CARTELLA (SOLO VISUALIZZAZIONE)
    // ----------------------------------------------------
    // Usa il colore passato dalla nota, altrimenti cerca in localStorage o usa default
    const startColor = coloreCartella || folderColors[cartellaVal] || "#ffb347";
    // Disabilitiamo immediatamente l'interazione col picker e sostituiamo
    // il pulsante colore con un nuovo elemento pulito per evitare che
    // residui di Pickr (event listeners, overlay, child nodes) lo rendano
    // modificabile o nascosto dopo azioni precedenti.
    window.allowFolderPickr = false;
    // Rimuove eventuale istanza Pickr e suoi listener
    resetPickr();

    const oldBtn = document.getElementById("folderColorBtn");
    const inputEl = document.getElementById("folderColorInput");

    // Crea un nuovo bottone pulito
    const newBtn = document.createElement("div");
    newBtn.id = "folderColorBtn";
    newBtn.className = "color-dot folder-preview";
    // Imposta solo il colore inline (il resto delle proprietà è nella classe)
    newBtn.style.backgroundColor = startColor;

    if (oldBtn && oldBtn.parentNode) {
      oldBtn.parentNode.replaceChild(newBtn, oldBtn);
    } else {
      // Se non trovato, proviamo ad inserirlo vicino all'input della cartella
      const cartWrapper = document.querySelector('.cartella-wrapper');
      if (cartWrapper) cartWrapper.appendChild(newBtn);
    }

    if (inputEl) inputEl.value = startColor;
    // Manteniamo la chiamata consolidata (con timeout) per assicurare
    // che tutta la UI venga aggiornata correttamente.
    applyFolderColor(startColor, false);
    // Aggiorna variabile globale
    selectedColor = startColor;
    // Aggiorna pickr
    if (window.folderPickr) window.folderPickr.setColor(startColor);

    
    // ----------------------------------------------------
    //  PULSANTE DUPLICA
    // ----------------------------------------------------
    const duplicaBtn = document.createElement("button");
    duplicaBtn.id = "duplicateBtn";
    duplicaBtn.className = "save-btn duplicate-btn";
    duplicaBtn.textContent = "📄 DUPLICA";
    duplicaBtn.onclick = () => duplicaNota(id);
    document.querySelector(".modal-actions").appendChild(duplicaBtn);

    // ----------------------------------------------------
    //  PULSANTE RIMUOVIMI (solo se non autore)
    // ----------------------------------------------------
    if (ruolo !== "autore") {
        const leaveBtn = document.createElement("button");
        leaveBtn.id = "leaveShareBtn";
        leaveBtn.className = "leave-btn";
        leaveBtn.textContent = "👋 Rimuovimi";
        leaveBtn.onclick = () => rimuovitiDallaNota(id);
        document.querySelector(".modal-actions").appendChild(leaveBtn);
    }

    // ----------------------------------------------------
    //  FOOTER INFO (creazione / ultima modifica)
    // ----------------------------------------------------
    const footer = document.createElement("p");
    footer.className = "note-footer";

    const autore = normUser(lastModifiedBy || autoreUsername || creatore || "autore sconosciuto");


    footer.textContent = lastModifiedAt
        ? `Ultima modifica – ${new Date(lastModifiedAt).toLocaleString("it-IT")} (${autore})`
        : createdAt
        ? `Creata il ${new Date(createdAt).toLocaleString("it-IT")} (${autore})`
        : `Creata da ${autore}`;

    document.querySelector(".modal-content").appendChild(footer);

    // ----------------------------------------------------
    //  PREPARA LA MODALE (rimuovi aria-hidden PRIMA)
    // ----------------------------------------------------
    modal.style.display = "flex";   // renderizza la modale
    modal.inert = false;            // rendila attiva
    modal.removeAttribute("aria-hidden"); // ora è visibile per assistive tech

    // ----------------------------------------------------
    // 12. SOLO ORA GESTIAMO IL FOCUS SICURO
    // ----------------------------------------------------
    modal.setAttribute("tabindex", "-1");
    setTimeout(() => modal.focus(), 0);
}



//=====================================================
// MODAL MODIFICA (con lock lato backend)
// =====================================================
async function apriModalModifica(
  id,
    titoloVal,
    contenutoVal,
    cartellaVal,
    ruolo = "autore",
    utentiCondivisi = [],
    permessoTipo = "Privata",
    lastModifiedAt = null,
    lastModifiedBy = null,
    createdAt = null,
    creatore = null,          
    coloreCartella = null,
    autoreUsername = null
) {
    editingNoteId = id;

     const versioneCorrente = window._noteVersions[id];
     window._versionLoadedAtOpen[id] = versioneCorrente;

    const locked = await startLock(id);
    if (!locked) {
      showToast("error", "🔒 Nota in modifica da un altro utente.");
      // aggiorna solo la card interessata così il bollino non resta verde
      try { await refreshCardLock(id); } catch (e) { /* ignore */ }
      return apriModalAnteprima(
          id,
          titoloVal,
          contenutoVal,
          cartellaVal,
          ruolo,
          utentiCondivisi,
          permessoTipo,
          lastModifiedAt,
          lastModifiedBy,
          createdAt,
          creatore,
          coloreCartella,
          autoreUsername 
      );
    }

    
    // Attiva subito il modal lato accessibilità
    modal.removeAttribute("aria-hidden");
    modal.inert = false;
    modalInitialized = false;

  // ----------------------------------------------------
  // CONTROLLO VERSIONE AGGIORNATA IN MODIFICA
  // ----------------------------------------------------
  try {
      const res = await fetch(`${API_NOTES}/${id}?user=${encodeURIComponent(user)}`);
      if (res.ok) {
          const notaServer = await res.json();
          const versioneServer = notaServer.versione;

          if (versioneServer !== versioneCorrente) {
              showToast(
                  "info",
                  "🔄 La nota è stata aggiornata da un altro utente. È stata caricata l'ultima versione."
              );

              // Aggiorna i valori che useremo nel modal
              titoloVal        = notaServer.titolo;
              contenutoVal     = notaServer.contenuto;
              cartellaVal      = notaServer.cartella;
              utentiCondivisi  = notaServer.utentiCondivisi || [];
              permessoTipo     = notaServer.permesso?.tipo || permessoTipo;
              lastModifiedAt   = notaServer.lastModifiedAt || lastModifiedAt;
              lastModifiedBy   = notaServer.lastModifiedBy || lastModifiedBy;
              createdAt        = notaServer.createdAt || createdAt;
              creatore         = notaServer.creatore || creatore;
              autoreUsername = notaServer.autoreUsername || notaServer.creatore || autoreUsername;

              // Aggiorna anche il tracking versione globale
              window._noteVersions[id] = versioneServer;
              window._versionLoadedAtOpen[id] = versioneServer;
          }
      }
  } catch (e) {
      console.warn("Errore controllo versione in modifica:", e);
  }

    // ----------------------------------------------------
    // RESET BOTTONE UTENTI (importante se si arriva da anteprima)
    // ----------------------------------------------------
    resetShareButton();

    // Reset UI utenti
    document.getElementById("utentiSelezionati").innerHTML = "";


    // =====================================================
    // PULIZIA MODALE
    // =====================================================
    document.querySelector("#duplicateBtn")?.remove();
    document.querySelector("#leaveShareBtn")?.remove();
    document.querySelector(".note-footer")?.remove();

    // =====================================================
    // POPOLAMENTO CAMPI
    // =====================================================
    titolo.value = titoloVal;
    contenuto.value = contenutoVal;
    cartella.value = cartellaVal || "";

    // 5B - MOSTRA AUTORE SOLO SE NON SEI TU
    if (modalAutore) {
        if (ruolo === "autore") {
            // L’autore NON deve vedere il campo autore
            modalAutore.style.display = "none";
            modalAutore.textContent = "";
        } else {
            // Utente condiviso, mostra chi è il creatore
            const autoreVisibile = autoreUsername || creatore || "autore sconosciuto";
            modalAutore.style.display = "block";
            modalAutore.textContent = `Autore: ${autoreVisibile}`;
        }
    }

    // PERMESSO NON EDITABILE
    const p = normalizzaPermesso(permessoTipo);
    permessoSelect.value = p;
    permessoSelect.disabled = true;

    // =====================================================
    // BANNER LOCK
    // =====================================================
    const lockBanner = document.getElementById("lockBanner");
    const lockUserSpan = document.getElementById("lockUser");
    const lockTimeSpan = document.getElementById("lockTime");

    // p = "privata" | "lettura" | "scrittura"
    const canShowLock = p === "scrittura" && (ruolo === "autore" || ruolo === "scrittura");

    if (!canShowLock) {
        if (lockBanner) lockBanner.style.display = "none";
    } else {
        if (lockBanner && lockUserSpan && lockTimeSpan) {
            lockUserSpan.textContent = user;
            lockTimeSpan.textContent = "ora";
            lockBanner.style.display = "block";

            let secondsPassed = 0;
            window.lockElapsedTimer = setInterval(() => {
                secondsPassed++;
                const m = Math.floor(secondsPassed / 60);
                const s = secondsPassed % 60;
                lockTimeSpan.textContent =
                    m > 0 ? `${m}m ${s}s` : `${s}s`;
            }, 1000);
        }
      }

    // COLORE CARTELLA
    const startColor = coloreCartella || folderColors[cartellaVal] || "#ffb347";
    const canEditColor = (ruolo === "autore" || p === "scrittura");
    // Aggiorna UI
    applyFolderColor(startColor, canEditColor);
    // Aggiorna variabile globale
    selectedColor = startColor;
    // Aggiorna pickr
    if (window.folderPickr) window.folderPickr.setColor(startColor);

    // =====================================================
    // LOGICA RUOLI
    // ====================================================
    if (ruolo === "autore") {
        modalTitle.textContent = "✏️ Modifica nota";
        titolo.disabled = false;
        contenuto.disabled = false;
        cartella.disabled = false;
        saveNoteBtn.style.display = "inline-block";
        saveNoteBtn.textContent = "💾 AGGIORNA";
    }
    else if (p === "lettura") {
        modalTitle.textContent = "👁️ Anteprima nota (lettura)";
        titolo.disabled = true;
        contenuto.disabled = true;
        cartella.disabled = true;
        saveNoteBtn.style.display = "none";

        const btn = document.getElementById("folderColorBtn");
        if (btn) btn.style.pointerEvents = "none";
    }
    else {
        modalTitle.textContent = "🖊️ Modifica nota condivisa";
        titolo.disabled = false;
        contenuto.disabled = false;
        cartella.disabled = false;
        saveNoteBtn.style.display = "inline-block";
        saveNoteBtn.textContent = "💾 AGGIORNA";
    }

    // ----------------------------------------------------
    //  BLOCCO BOTTONE UTENTI PER CHI NON È AUTORE
    // ----------------------------------------------------
    if (ruolo !== "autore") {
        // nascondi completamente il bottone
        shareNoteBtn.disabled = true;
        shareNoteBtn.classList.add("disabled-preview");
        shareNoteBtn.style.pointerEvents = "none";
        
        // optional: toast se cliccato
        shareNoteBtn.onclick = (e) => {
            e.stopPropagation();
            showToast("error", "Solo l'autore può gestire gli utenti condivisi.");
        };
    }
        cancelNoteBtn.style.display = "inline-block";

    // =====================================================
    // SEZIONE CONDIVISIONE + PULSANTE UTENTI
    // =====================================================
    const isPrivata = p === "privata";
    shareSection.style.display = isPrivata ? "none" : "flex";

    if (!isPrivata) {
        shareNoteBtn.style.display = "inline-block";
        shareNoteBtn.className = "share-users-btn";
        shareNoteBtn.innerHTML = "👥 Utenti";
    } else {
        shareNoteBtn.style.display = "none";
    }

    // =====================================================
    // PILLOLE UTENTI
    // =====================================================
    const usersContainer = document.getElementById("utentiSelezionati");
    usersContainer.innerHTML = "";
    utentiSelezionati = [...(utentiCondivisi || [])];

    if (!isPrivata) {
      if (utentiSelezionati.length === 0) {
        usersContainer.innerHTML = "<span style='color:#777;'>nessuno</span>";
      } else {
        utentiSelezionati.forEach(u => {
          const pill = document.createElement("span");
          pill.className = "user-pill-inline";
          pill.textContent = normUser(u);
          usersContainer.appendChild(pill);
        });
      }
    }

    // =====================================================
    // FOOTER INFO
    // =====================================================
    const footer = document.createElement("p");
    footer.className = "note-footer";

    const autore = normUser(lastModifiedBy || autoreUsername || creatore || "autore sconosciuto");

    footer.textContent =
        lastModifiedAt
            ? `Ultima modifica – ${new Date(lastModifiedAt).toLocaleString("it-IT")} (${autore})`
            : createdAt
            ? `Creata il ${new Date(createdAt).toLocaleString("it-IT")} (${autore})`
            : `Creata da ${autore}`;

    document.querySelector(".modal-content").appendChild(footer);

    // =====================================================
    // MOSTRA MODALE (ordine corretto per ARIA + focus)
    // =====================================================
    modal.style.display = "flex";
    modal.inert = false;
    modal.removeAttribute("aria-hidden");

    // Focus sicuro (evita warning aria-hidden)
    modal.setAttribute("tabindex", "-1");
    setTimeout(() => modal.focus(), 0);
   }


async function chiudiModal(id) {
  const modalEl = document.getElementById(id);
  if (!modalEl) return;

  document.activeElement?.blur();

  modalEl.style.display = "none";
  modalEl.setAttribute("aria-hidden", "true");
  modalEl.inert = true;
  modalInitialized = false;

  if (id === "noteModal") {

    // Ferma timer banner lock
    if (window.lockElapsedTimer) {
      clearInterval(window.lockElapsedTimer);
      window.lockElapsedTimer = null;
    }

    // Ferma lock-system
    if (typeof stopLock === "function") {
      stopLock();
    }

    // Sblocca la nota SOLO se era realmente lockata dal frontend
    if (
      typeof apiUnlock === "function" &&
      lockActive &&
      currentLockedNoteId === editingNoteId
    ) {
      await apiUnlock(editingNoteId);
    }

    // Reset lock vars
    lockActive = false;
    currentLockedNoteId = null;

    // ===  RIPRISTINO STATO ORIGINALE NOTA (solo se non salvata) ===
    if (editingNoteId != null && window._originalNoteSnapshot && window._lastOpenedNote) {
      const snap = window._originalNoteSnapshot;
      const nota = window._lastOpenedNote;

      nota.titolo = snap.titolo;
      nota.contenuto = snap.contenuto;
      nota.cartella = snap.cartella;
      nota.coloreCartella = snap.coloreCartella;
      nota.lastModifiedAt = snap.lastModifiedAt;
      nota.lastModifiedBy = snap.lastModifiedBy;
    }

    // Reset ID nota in editing
    editingNoteId = null;

    // Reset banner lock UI
    const lockBanner = document.getElementById("lockBanner");
    const lockUserSpan = document.getElementById("lockUser");
    const lockTimeSpan = document.getElementById("lockTime");

    if (lockBanner) lockBanner.style.display = "none";
    if (lockUserSpan) lockUserSpan.textContent = "";
    if (lockTimeSpan) lockTimeSpan.textContent = "";

    // Reset info autore
    const modalAutore = document.getElementById("modalAutore");
    if (modalAutore) {
      modalAutore.style.display = "none";
      modalAutore.textContent = "";
    }

    // Reset snapshot
    window._originalNoteSnapshot = null;
    window._lastOpenedNote = null;
  }
}


// =====================================================
// CHIUSURA SHARE MODAL (indipendente dal lock)
// =====================================================
function chiudiShareModal() {
    const modal = document.getElementById("shareModal");
    if (!modal) return;

    // Sposta focus via, evita attivazioni involontarie
    document.activeElement?.blur();

    // Chiudi davvero il modal utenti
    modal.style.display = "none";
    modal.setAttribute("aria-hidden", "true");
    modal.inert = true;

    // RIATTIVA il modal principale delle note  
    const noteModal = document.getElementById("noteModal");
    if (noteModal) {
        noteModal.inert = false;
        noteModal.removeAttribute("aria-hidden");

        // Rimettere il focus SUL MODAL DELLA NOTA, non su elementi interni
        noteModal.setAttribute("tabindex", "-1");
        setTimeout(() => noteModal.focus(), 0);
    }
}



// =====================================================
//  SALVATAGGIO NOTE (Create + Update allineato ai DTO)
// =====================================================
if (saveNoteBtn) {
  saveNoteBtn.addEventListener("click", async () => {
    const titoloEl = document.getElementById("titolo");
    const contenutoEl = document.getElementById("contenuto");
    const cartellaEl = document.getElementById("cartella");
    const folderColorInput = document.getElementById("folderColorInput");

    if (!titoloEl || !contenutoEl || !cartellaEl || !folderColorInput) {
      showToast("error", "Errore interno: campo mancante.");
      return;
    }

    const titolo = titoloEl.value.trim();
    const contenuto = contenutoEl.value.trim();
    const cartella = cartellaEl.value.trim();
    const coloreCartella = folderColorInput.value || "#ffb347";

    let permesso = (permessoSelect?.value || "privata").toLowerCase();
    if (permesso.includes("lettura")) permesso = "lettura";
    if (permesso.includes("scrittura")) permesso = "scrittura";
    if (permesso.includes("privata")) permesso = "privata";

    if (!titolo || !contenuto) {
      showToast("info", "⚠️ Inserisci titolo e contenuto!");
      return;
    }

    const isUpdate = !!editingNoteId;

    let body;
    if (isUpdate) {
      // DTO NoteUpdateRequest
      body = {
        titolo,
        contenuto,
        cartella: cartella || null,
        coloreCartella: coloreCartella,
        versionExpected: window._versionLoadedAtOpen?.[editingNoteId] ?? null
      };
    } else {
      // DTO CreateNoteRequest
      body = {
        titolo,
        contenuto,
        creatore: user,
        cartella: cartella || null,
        utentiCondivisi: utentiSelezionati,
        permesso,
        coloreCartella: coloreCartella
      };
    }
    
    // Salva il colore della cartella in locale
    if (cartella) {
      localStorage.setItem(`folderColor_${cartella}`, coloreCartella);
      folderColors[cartella] = coloreCartella;

    }

    // Chiamata API
    const method = isUpdate ? "PUT" : "POST";
    const url = isUpdate
      ? `${API_NOTES}/${Number(
          editingNoteId
        )}?user=${encodeURIComponent(user)}`
      : API_NOTES;

    
try {
    const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });

    if (res.ok) {

        // SE È UNA CREAZIONE, SALVIAMO LA STORIA DELL'UTENTE
        if (!isUpdate && !hasEverCreatedNote) {
            hasEverCreatedNote = true;
            localStorage.setItem(`hasEverCreatedNote_${user}`, "true");
        }

        showToast(
            "success",
            isUpdate ? " Nota aggiornata!" : " Nota creata con successo!"
        );

        await new Promise(r => setTimeout(r, 150));
        await caricaNote();

    } else {
        const msg = await res.text();
        console.error("❌ Errore backend:", msg);

        // GESTIONE SPECIFICA VERSIONE CONFLICT (409)
        if (res.status === 409) {
            if (msg.includes("aggiornata da un altro utente")) {
                showToast(
                    "error",
                    "⚠️ La nota è stata aggiornata da un altro utente.\nRiaprila per continuare senza perdere dati."
                );
            } else if (msg.includes("lock") || msg.includes("modifica da")) {
                showToast("error", msg.replace("❌", "").trim());
            } else {
                showToast("error", `❌ Conflitto: ${msg}`);
            }
        } else {
            // gestione generica errori
            showToast(
                "error",
                `Errore durante il salvataggio della nota (${res.status})`
            );
        }
    }
} catch (err) {
    console.error("⚠️ Errore di rete o fetch:", err);
    showToast("error", "Errore di connessione al server.");
} finally {
    chiudiModal("noteModal");
}
  });
}


// =====================================================
//  ELIMINAZIONE NOTE (con controllo LOCK + conferma)
// =====================================================
async function eliminaNota(id) {

  // Verifica subito se la nota è lockata da un altro utente
  try {
    const resLock = await fetch(`${API_NOTES}/${id}/lock`);
    const data = await resLock.json();

    if (data.lockedBy && data.lockedBy !== user) {
      showToast("error", `🔒 Nota in modifica da ${data.lockedBy}. Non puoi eliminarla ora.`);
      return;
    }
  } catch (err) {
    console.warn("Errore verifica lock:", err);
    showToast("error", "Impossibile verificare lo stato della nota.");
    return;
  }

  //   Se nota NON è lockata da altri, procedi con la conferma
  showConfirmToast(
    "Vuoi davvero eliminare questa nota?",
    async () => {
      try {
        const res = await fetch(
          `${API_NOTES}/${id}?user=${encodeURIComponent(user)}`,
          { method: "DELETE" }
        );

        if (res.ok) {
          showToast("success", "🗑️ Nota eliminata!");
          caricaNote();
        } else if (res.status === 403) {
          showToast("error", "❌ Solo l'autore può eliminare questa nota.");
        } else if (res.status === 409) {
          const msg = await res.text();
          showToast("error", `❌ Impossibile eliminare: la nota è in modifica.\n${msg}`);
        } else {
          showToast("error", "❌ Errore durante l'eliminazione della nota.");
        }
      } catch (err) {
        console.error("Errore eliminazione nota:", err);
        showToast("error", "Errore di connessione durante l'eliminazione.");
      }
    },
    () => {
      showToast("info", "Operazione annullata.");
    }
  );
}


// Rimuovere se stessi da una nota condivisa (toast di conferma)
async function rimuovitiDallaNota(id) {

  showConfirmToast(
    "Vuoi rimuovere questa nota dalla tua vista?",
    async () => {
      try {
        const res = await fetch(`${API_NOTES}/${id}/removeSelf`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ user })
              });

        if (res.ok) {
          // Chiudi il modal prima di ricaricare
          const modal = document.getElementById("noteModal");
          modal.style.display = "none";
          
          // Pulisci il cache della nota da localStorage
          localStorage.removeItem(`noteInfo_${id}`);
          const sharedIds = JSON.parse(localStorage.getItem(`sharedNotes_${user}`) || "[]");
          const updated = sharedIds.filter(nid => nid !== id);
          localStorage.setItem(`sharedNotes_${user}`, JSON.stringify(updated));
          
          showToast("success", "👋 Ti sei rimosso dalla nota condivisa.");
          caricaNote();
        } else {
          showToast("error", "❌ Errore durante la rimozione dalla nota condivisa.");
        }
      } catch (err) {
        console.error("Errore rimozione nota condivisa:", err);
        showToast("error", "Errore di connessione durante la rimozione.");
      }
    },
    () => {
      showToast("info", "Operazione annullata.");
    }
  );
}

// =====================================================
//  VERSIONI
// =====================================================
async function mostraVersioni(id) {
  const versionModal = document.getElementById("versionModal");
  const versionListModal = document.getElementById("versionListModal");
  const noteModal = document.getElementById("noteModal");
  const shareModal = document.getElementById("shareModal");

  if (!versionModal || !versionListModal) return;

  //  Disattiva gli altri modal
  if (noteModal) {
    noteModal.setAttribute("aria-hidden", "true");
    noteModal.inert = true;
  }
  if (shareModal) {
    shareModal.setAttribute("aria-hidden", "true");
    shareModal.inert = true;
  }

  //  Attiva la modale delle versioni
  versionModal.style.display = "flex";
  versionModal.removeAttribute("aria-hidden");
  versionModal.inert = false;

   const parent = versionModal.parentElement;
  if (parent && parent.inert) {
    parent.inert = false;
  }

  versionListModal.innerHTML = "<p>Caricamento versioni...</p>";

  // ==============================
  //   CARICAMENTO VERSIONI
  // ==============================
  try {
    const res = await fetch(`${API_NOTES}/${id}?user=${encodeURIComponent(user)}`);
    if (!res.ok) {
      versionListModal.innerHTML =
        "<p>Errore nel caricamento delle versioni.</p>";
      return;
    }

    const nota = await res.json();

    if (!nota.versioni || nota.versioni.length === 0) {
      versionListModal.innerHTML = "<p>Nessuna versione salvata.</p>";
      return;
    }

    versionListModal.innerHTML = "";

    // Numero totale versioni memorizzate
    const totalVersions = nota.versioni.length;

    nota.versioni.forEach((v, i) => {
      const timeLabel = tempoRelativo(v.timestamp);

      // Versione visuale (inversa)
      const visualNumber = totalVersions - i;

      const div = document.createElement("div");
      div.className = "version-item";

      div.innerHTML = `
        <small>Versione #${visualNumber} — ${timeLabel}</small>
        <h3>📝 ${v.titolo || "(Titolo mancante)"}</h3>
        <p>${v.contenuto || "(Contenuto vuoto)"}</p>
        <button class="restore-btn">Ripristina</button>
      `;


      const restoreBtn = div.querySelector(".restore-btn");
    
     
      const isLockedByOther = nota.lockedBy && nota.lockedBy !== user;

      if (isLockedByOther) {
        restoreBtn.classList.add("disabled-btn");
        restoreBtn.onclick = (e) => {
          e.stopPropagation();
          showToast("error", `🔒 Ripristino non disponibile. Nota bloccata da ${nota.lockedBy}.`);
        };
      } else {
   
        restoreBtn.addEventListener("click", () => ripristinaVersione(id, i));
      }

      versionListModal.appendChild(div);
    });

  } catch (err) {
    console.error("Errore caricamento versioni:", err);
    versionListModal.innerHTML = "<p>Errore di connessione.</p>";
  }
}


const closeVersionBtn = document.getElementById("closeVersionBtn");

if (closeVersionBtn) {
  closeVersionBtn.addEventListener("click", () => {
    chiudiModal("versionModal");

    const noteModal = document.getElementById("noteModal");
    const shareModal = document.getElementById("shareModal");

    if (noteModal) {
      noteModal.inert = false;
      noteModal.removeAttribute("aria-hidden");
    }

    if (shareModal) {
      shareModal.inert = false;
      shareModal.removeAttribute("aria-hidden");
    }
  });
}

function tempoRelativo(timestampISO) {
  const then = new Date(timestampISO);
  const diffMs = Date.now() - then.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  const diffOre = Math.floor(diffMin / 60);
  const diffGiorni = Math.floor(diffOre / 24);
  if (diffMin < 1) return "ora";
  if (diffMin < 60) return `${diffMin} min fa`;
  if (diffOre < 24) return `${diffOre} ore fa`;
  return `${diffGiorni} giorni fa`;
}

async function ripristinaVersione(id, index) {

// =============================================
//  BLOCCO RIPRISTINO SE LOCKATA
// =============================================
const resLock = await fetch(`${API_NOTES}/${id}?user=${encodeURIComponent(user)}`);
const nota = await resLock.json();
if (nota.lockedBy && nota.lockedBy !== user) {
    showToast("error", `🔒 Nota in modifica da ${nota.lockedBy}. Non puoi ripristinare.`);
    return;
}

  showConfirmToast(
    `Vuoi ripristinare la versione #${index + 1}?`,
    async () => {
      try {
        const res = await fetch(
          `${API_NOTES}/${id}/restore/${index}?user=${encodeURIComponent(user)}`,
          { method: "PUT" }
        );

        if (res.ok) {
          showToast("success", "🔙 Versione ripristinata!");
          chiudiModal("versionModal");
          const notaAggiornata = await fetch(`${API_NOTES}/${id}?user=${encodeURIComponent(user)}`).then(r => r.json());
          loadFolderColors();
          await caricaNote();
        } else if (res.status === 403) {
          showToast("error", "❌ Solo l'autore può ripristinare versioni precedenti.");
        } else {
          showToast("error", "❌ Errore nel ripristino della versione.");
        }
      } catch (err) {
        console.error("Errore ripristino versione:", err);
        showToast("error", "Errore di connessione durante il ripristino.");
      }
    },
    () => {
      showToast("info", "Ripristino versione annullato.");
    }
  );
}


// =====================================================
//  CONDIVISIONE NOTE (selezione utenti, non chiama API)
// =====================================================
const shareModal = document.getElementById("shareModal");
const userListShare = document.getElementById("userListShare");
const shareSaveBtn = document.getElementById("shareSaveBtn");
const shareCancelBtn = document.getElementById("shareCancelBtn");

if (shareNoteBtn) {
  shareNoteBtn.addEventListener("click", async () => {
    if (!shareModal || !userListShare) return;

    shareModal.style.display = "flex";
    
    shareModal.removeAttribute("aria-hidden");
    shareModal.inert = false;

    modal.setAttribute("aria-hidden", "true");
    modal.inert = true;

    
    shareModal.setAttribute("tabindex", "-1");
    setTimeout(() => shareModal.focus(), 0);

    userListShare.innerHTML = "<p>Caricamento utenti...</p>";

    try {
      const res = await fetch(API_USERS);
      if (!res.ok) {
        console.error("Errore caricamento utenti:", res.status, res.statusText);
        userListShare.innerHTML = "<p>⚠️ Errore nel caricamento utenti.</p>";
        return;
      }

      const utenti = await res.json();
      userListShare.innerHTML = "";

      utenti.forEach((nome) => {

        if (!nome || nome.trim().toLowerCase() === user.trim().toLowerCase()) {
          return;
        }

        const riga = document.createElement("div");
        riga.className = "user-item";

        const cb = document.createElement("input");
        cb.type = "checkbox";
        cb.value = nome;
        cb.style.display = "none";

        const pill = document.createElement("div");
        pill.className = "user-pill";
        pill.textContent = normUser(nome);

        if (utentiSelezionati.includes(nome)) {
          cb.checked = true;
          riga.classList.add("selected");
        }

        pill.addEventListener("click", () => {
          cb.checked = !cb.checked;
          toggleUtente(cb, nome, riga);
        });

        riga.appendChild(cb);
        riga.appendChild(pill);
        userListShare.appendChild(riga);

      }); 

      if (userListShare.children.length === 0) {
        userListShare.innerHTML =
          "<p style='opacity:0.8;'>Nessun altro utente disponibile.</p>";
      }

    } catch (err) {
      console.error("Errore caricamento utenti:", err);
      userListShare.innerHTML = "<p>⚠️ Errore di connessione.</p>";
    }
  });
}


function toggleUtente(checkbox, nome, riga) {
  const index = utentiSelezionati.indexOf(nome);

  if (checkbox.checked && index === -1) {
    utentiSelezionati.push(nome);
    if (riga) riga.classList.add("selected");
  } else if (!checkbox.checked && index !== -1) {
    utentiSelezionati.splice(index, 1);
    if (riga) riga.classList.remove("selected");
  }

  renderUtentiCondivisi(utentiSelezionati);

}

if (shareCancelBtn) {
  shareCancelBtn.addEventListener("click", chiudiShareModal);
}

shareSaveBtn.addEventListener("click", () => {
    renderUtentiCondivisi(utentiSelezionati);
    chiudiShareModal();
});


// =====================================================
//  FILTRO NOTE IN TEMPO REALE
// =====================================================
const searchInput = document.getElementById("searchInput");

if (searchInput) {
  searchInput.addEventListener("input", () => {
    const query = searchInput.value.toLowerCase().trim();
    const cards = document.querySelectorAll(".note-card");
    const noResults = document.getElementById("noResults");

    let found = false;

    cards.forEach((card) => {
      const titolo = card.querySelector("h2")?.textContent.toLowerCase() || "";
      const pElements = card.querySelectorAll("p");
      const contenuto = pElements[0]?.textContent.toLowerCase() || "";
      const cartella =
        card.querySelector("p small")?.textContent.toLowerCase() || "";

      if (
        titolo.includes(query) ||
        cartella.includes(query) ||
        contenuto.includes(query)
      ) {
        card.style.display = "flex";
        found = true;
      } else {
        card.style.display = "none";
      }
    });

    if (!noResults) return;

    if (!found && query !== "") {
      noResults.style.display = "flex";
    } else {
      noResults.style.display = "none";
    }
  });
}

// =====================================================
// DUPLICA NOTA
// =====================================================
async function duplicaNota(id) {
  try {
    // Recupera la nota originale
    const res = await fetch(`${API_NOTES}/${id}?user=${encodeURIComponent(user)}`);
    if (!res.ok) {
      showToast("error", "❌ Impossibile recuperare la nota da duplicare.");
      return;
    }

    const nota = await res.json();

    // 2. FORZA MODAL IN MODALITÀ CREA
    editingNoteId = null;          
    utentiSelezionati = [];        

    document.querySelector("#duplicateBtn")?.remove();
    document.querySelector("#leaveShareBtn")?.remove();
    document.querySelector(".note-footer")?.remove();

    if (shareNoteBtn) {
      shareNoteBtn.disabled = false;
      shareNoteBtn.onclick = null;                 
      shareNoteBtn.classList.remove("disabled-preview");
      shareNoteBtn.style.display = "inline-block";
    }

    modalInitialized = true;
    modal.removeAttribute("aria-hidden");
    modal.inert = false;

    // Mostra la modale
    modal.style.display = "flex";
    modalTitle.textContent = "📄 Duplica nota";

    
    if (modalAutore) {
        modalAutore.style.display = "none";
        modalAutore.textContent = "";
    }

    // Pulsanti corretti
    saveNoteBtn.style.display = "inline-block"; 
    saveNoteBtn.textContent = "💾 CREA COPIA";

    cancelNoteBtn.style.display = "inline-block";
    shareNoteBtn.style.display = "inline-block"; 

    // 3. ATTIVA I CAMPI 
    titolo.disabled = false;
    contenuto.disabled = false;
    cartella.disabled = false;
    permessoSelect.disabled = false;

    // 4. PRECOMPILA I CAMPI
    titolo.value = nota.titolo + " (Copia)";
    contenuto.value = nota.contenuto;
    cartella.value = nota.cartella || "";

    permessoSelect.value = "privata";   
    shareSection.style.display = "none";
    renderUtentiCondivisi([]);

    // 5. COLORE CARTELLA
    const colore = nota.coloreCartella || "#ffb347";

    if (typeof applyFolderColor === 'function') {
      applyFolderColor(colore, true);

      const inputColore = document.getElementById("folderColorInput");
      if (inputColore) inputColore.value = colore;
    } else {
      
      setTimeout(() => {
        const inputColore = document.getElementById("folderColorInput");
        const btnColore = document.getElementById("folderColorBtn");

        if (inputColore) inputColore.value = colore;
        if (btnColore) btnColore.style.backgroundColor = colore;

        resetPickr();
        initFolderColorPicker(colore);
      }, 120);
    }

  } catch (err) {
    console.error("Errore duplicazione:", err);
    showToast("error", "❌ Errore durante la duplicazione.");
  }
}

// =====================================================
// MINI TUTORIAL
// =====================================================
const tutorialSideBtn = document.getElementById("tutorialSideBtn");
if (tutorialSideBtn) {
  tutorialSideBtn.addEventListener("click", () => {
    localStorage.setItem(`tutorialShown_${user}`, "true");

    const side = document.getElementById("tutorialSide");
    if (side) side.style.display = "none";

    document.body.classList.remove("tutorial-mode");

    document.querySelectorAll(".note-card").forEach(card => {
    card.style.opacity = "1";
});

    aggiornaToolsPanel();
    caricaNote();
  });
}

// =====================================================
// CONVERSIONE COLORE HEX → HUE
// =====================================================
function hexToHue(hex) {
  if (!hex) return 0;

  const { r, g, b } = hexToRgb(hex);
  const { h } = rgbToHsl(r, g, b);
  return Math.round(h);
}

function hexToRgb(hex) {
  hex = hex.replace("#", "");
  if (hex.length === 3) hex = hex.split("").map(x => x + x).join("");
  const n = parseInt(hex, 16);
  return {
    r: (n >> 16) & 255,
    g: (n >> 8) & 255,
    b: n & 255
  };
}

function rgbToHsl(r, g, b) {
  r /= 255; g /= 255; b /= 255;
  const max = Math.max(r, g, b), min = Math.min(r, g, b);
  let h, s, l = (max + min) / 2;
  if (max === min) {
    h = s = 0;
  } else {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    switch (max) {
      case r: h = (g - b) / d + (g < b ? 6 : 0); break;
      case g: h = (b - r) / d + 2; break;
      case b: h = (r - g) / d + 4; break;
    }
    h /= 6;
  }
  return { h: h * 360, s, l };
}

// AVVIO INIZIALE
caricaNote().then(() => {
    checkNewFolderParam();
});
