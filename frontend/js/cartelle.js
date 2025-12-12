const API_NOTES = "http://localhost:8080/api/notes";
const user = localStorage.getItem("loggedUser");

const folderList = document.getElementById("folderItems");
const noteList = document.getElementById("noteList");
const searchInput = document.getElementById("searchInput");
const noResults = document.getElementById("noResults");
const welcomeUser = document.getElementById("welcomeUser");

// messaggio "Seleziona una cartella"
const noFolderSelected = document.getElementById("noFolderSelected");

// === TRACKING VERSIONI (come in dashboard.js) ===
window._noteVersions = window._noteVersions || {};
window._versionLoadedAtOpen = window._versionLoadedAtOpen || {};


function openNoteModal(nota) {
  const modal = document.getElementById("previewModal");
  const modalTitle = document.getElementById("previewTitolo");
  const modalContent = document.getElementById("previewContenuto");

  if (!modal) {
    console.error("Modal not found");
    return;
  }

  // Controllo dei dati della nota
  if (!nota || !nota.titolo || !nota.contenuto) {
    console.error("Dati della nota non validi", nota);
    return;
  }

  // Mostra la modale
  modal.style.display = "flex";  // Assicurati che sia visibile con 'flex'

  // Imposta il contenuto della modale con il titolo e il contenuto della nota
  modalTitle.textContent = nota.titolo;
  modalContent.textContent = nota.contenuto;

  // Aggiungi un evento per chiudere la modale quando clicchi fuori
  modal.addEventListener("click", (e) => {
    if (e.target === modal || e.target === closePreviewBtn) {
      modal.style.display = "none";
    }
  });
}


// =====================================================
//  TOAST BASE
// =====================================================
function showToast(type, message) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.textContent = message;

  container.appendChild(toast);

  setTimeout(() => toast.remove(), 4000);
}


// =====================================================
//  TOAST DI CONFERMA (SÌ / NO)
// =====================================================
function showConfirmToast(message, onConfirm, onCancel = null) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = "toast toast-confirm";
  toast.style.padding = "16px";
  toast.style.display = "flex";
  toast.style.flexDirection = "column";
  toast.style.alignItems = "center";
  toast.style.gap = "10px";
  toast.style.whiteSpace = "normal";

  const msg = document.createElement("div");
  msg.textContent = message;

  const btnBox = document.createElement("div");
  btnBox.style.display = "flex";
  btnBox.style.gap = "8px";

  const yesBtn = document.createElement("button");
  yesBtn.textContent = "Sì";
  yesBtn.className = "toast-btn toast-btn-ok";

  const noBtn = document.createElement("button");
  noBtn.textContent = "No";
  noBtn.className = "toast-btn toast-btn-cancel";

  yesBtn.onclick = () => {
    toast.remove();
    if (onConfirm) onConfirm();
  };

  noBtn.onclick = () => {
    toast.remove();
    if (onCancel) onCancel();
  };

  btnBox.appendChild(yesBtn);
  btnBox.appendChild(noBtn);

  toast.appendChild(msg);
  toast.appendChild(btnBox);

  container.appendChild(toast);
}


// =====================================================
// TOAST CON INPUT (per creare cartelle senza prompt)
// =====================================================
function showInputToast(message, placeholder, onConfirm, onCancel = null) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = "toast toast-input";
  toast.style.display = "flex";
  toast.style.flexDirection = "column";
  toast.style.alignItems = "center";
  toast.style.gap = "10px";

  const msg = document.createElement("div");
  msg.textContent = message;

  const input = document.createElement("input");
  input.type = "text";
  input.placeholder = placeholder;
  input.className = "toast-input-field";

  const btnBox = document.createElement("div");
  btnBox.style.display = "flex";
  btnBox.style.gap = "8px";

  const yesBtn = document.createElement("button");
  yesBtn.textContent = "OK";
  yesBtn.className = "toast-btn toast-btn-ok";

  const noBtn = document.createElement("button");
  noBtn.textContent = "Annulla";
  noBtn.className = "toast-btn toast-btn-cancel";

  yesBtn.onclick = () => {
    const value = input.value.trim();
    toast.remove();
    if (value && onConfirm) onConfirm(value);
  };

  noBtn.onclick = () => {
    toast.remove();
    if (onCancel) onCancel();
  };

  btnBox.appendChild(yesBtn);
  btnBox.appendChild(noBtn);

  toast.appendChild(msg);
  toast.appendChild(input);
  toast.appendChild(btnBox);

  container.appendChild(toast);
}

if (!user) {
  showToast("error", "Devi effettuare il login.");
  setTimeout(() => window.location.replace("auth.html"), 1200);
}


if (welcomeUser) {
  welcomeUser.textContent = `Ciao, ${user.toUpperCase()}! 👋`;
}

// ===== LOGOUT ===== */
const logoutBtn = document.getElementById("logoutBtn");
if (logoutBtn) {
  logoutBtn.addEventListener("click", () => {
    localStorage.removeItem("loggedUser");
    window.location.replace("auth.html");
  });
}

/* ===== TORNA ALLA DASHBOARD ===== */
const backDashboardBtn = document.getElementById("backDashboardBtn");

if (backDashboardBtn) {
  backDashboardBtn.addEventListener("click", () => {
    window.location.href = "dashboard.html";
  });
}

// =====================================================
//  CONTROLLO VERSIONE COME DASHBOARD
// =====================================================
async function ensureLatestVersion(nota, user) {
    try {
        const res = await fetch(`${API_NOTES}/${nota.id}?user=${encodeURIComponent(user)}`);
        if (!res.ok) return nota;

        const fresh = await res.json();

        const nuova = fresh.versione;
        const vecchia = window._noteVersions[nota.id];

        // se non è cambiata, torna quella attuale
        if (nuova === vecchia) return nota;

        //  versione aggiornata,  notifica
        showToast("info", `🔄 La nota è stata aggiornata da ${fresh.lastModifiedBy}.`);

        // aggiorna nota locale
        Object.assign(nota, {
            titolo: fresh.titolo,
            contenuto: fresh.contenuto,
            cartella: fresh.cartella,
            coloreCartella: fresh.coloreCartella,
            permesso: fresh.permesso,
            condivisaCon: fresh.condivisaCon,
            ruolo: fresh.ruolo,
            lastModifiedAt: fresh.lastModifiedAt,
            lastModifiedBy: fresh.lastModifiedBy,
            creatore: fresh.creatore,
            autoreUsername: fresh.autoreUsername,
            versione: fresh.versione,
        });

        // aggiorna cache versioni
        window._noteVersions[nota.id] = fresh.versione;

        // ricarica la UI della pagina cartelle
        await caricaNote();

        return nota;

    } catch (e) {
        console.warn("Errore controllo versione cartelle:", e);
        return nota;
    }
}

let tutteLeNote = [];
let cartellaAttiva = null;

/* ====== LOCK INFO (solo lettura — per impedire eliminazione) ====== */
async function getLockInfo(noteId) {
  try {
    const res = await fetch(`${API_NOTES}/${noteId}/lock`);
    if (!res.ok) return null;
    return await res.json(); // { lockedBy: "...", lockedAt: ... }
  } catch (e) {
    console.error("Errore lock info:", e);
    return null;
  }
}


/* ===== CARICA TUTTE LE NOTE ===== */
async function caricaNote() {
  try {
    const res = await fetch(`${API_NOTES}/visible/${encodeURIComponent(user)}`);
    tutteLeNote = await res.json();
    generaCartelle();
    mostraNote();
  } catch (err) {
    console.error("Errore caricamento note:", err);
  }
}

/* ===== CREA LISTA CARTELLE ===== */
function generaCartelle() {

  const cartelle = [...new Set(
    tutteLeNote
      .filter(n => n.cartella && n.cartella.trim() !== "")
      .map(n => n.cartella.trim())
  )].sort((a, b) => a.localeCompare(b));

  folderList.innerHTML = "";

  cartelle.forEach(nome => {
    const li = document.createElement("li");
    li.className = "folder-row";

    // recuperiamo il colore della cartella (prendiamo la prima nota che la usa)
    const coloreCartella = tutteLeNote.find(n => n.cartella === nome)?.coloreCartella || "#cccccc";

    // icona + nome cartella
    li.innerHTML = `
        <svg class="folder-icon-list" style="color:${coloreCartella};">
            <use href="#folder-fill"></use>
            <use href="#folder-stroke"></use>
        </svg>
        ${nome}
    `;

    // salviamo il colore dentro dataset, ci servirà al click
    li.dataset.colore = coloreCartella;

    li.addEventListener("click", () => {
      // Nasconde il messaggio iniziale
      noFolderSelected.classList.add("hidden");

      // reset tutte le cartelle
      document.querySelectorAll("#folderItems li").forEach(el => {
          el.classList.remove("active");
          el.style.backgroundColor = "";
          el.style.borderLeft = "";
      });

      // attiva questa
      li.classList.add("active");

      // colore dinamico (riempimento e bordo)
      const bg = li.dataset.colore;
      const bgLight = lighten(bg, 120);   
      const bgBorder = darken(bg, 35);    

      li.style.backgroundColor = bgLight;
      li.style.borderLeft = `4px solid ${bgBorder}`;
      li.style.color = "#000";            

      cartellaAttiva = nome;
      mostraNote();
    });

    folderList.appendChild(li);
  });
}

function mostraNote() {
  noteList.innerHTML = "";

  // SE NON C'È CARTELLA SELEZIONATA,  mostra il messaggio
  if (!cartellaAttiva) {
    noFolderSelected.classList.remove("hidden");
    noResults.style.display = "none";
    return;
  }

  // quando una cartella è selezionata si nasconde il messaggio
  noFolderSelected.classList.add("hidden");

  const testoRicerca = searchInput.value.toLowerCase();
  let filtrate = tutteLeNote.filter(n => n.cartella === cartellaAttiva);

  if (testoRicerca) {
    filtrate = filtrate.filter(n =>
      (n.titolo || "").toLowerCase().includes(testoRicerca) ||
      (n.contenuto || "").toLowerCase().includes(testoRicerca)
    );
  }

  if (filtrate.length === 0) {
    noResults.style.display = "block";
    return;
  }

  noResults.style.display = "none";

  filtrate.forEach(nota => {
  const card = document.createElement("div");
  card.className = "note-card";

  // Aggiungi un evento per aprire la nota in versione estesa (modale o sezione)
  card.addEventListener("click", () => {
    openNoteModal(nota);  // La modale si apre solo quando l'utente clicca sulla carta
  });

  // ==========================
  //  PERMESSO NORMALIZZATO
  // ==========================
  let permessoTipo = "privata";

  if (!nota.permesso) {
    permessoTipo = "privata";
  } else if (typeof nota.permesso === "string") {
    permessoTipo = nota.permesso.toLowerCase();
  } else if (nota.permesso && nota.permesso.tipo) {
    permessoTipo = nota.permesso.tipo.toLowerCase();
  }

  // ==========================
  //  COLORE BORDO CARD
  // ==========================
  if (permessoTipo.includes("scrittura")) {
    card.classList.add("shared-write");
  } else if (permessoTipo.includes("lettura")) {
    card.classList.add("shared-read");
  } else {
    card.classList.add("note-private");
  }

  // ==========================
  //  VERSIONE (usa campo versione SE esiste)
  // ==========================
  const versioneCorrente =
    typeof nota.versione === "number"
      ? nota.versione
      : (nota.versioni?.length || 0) + 1;

  // ==========================
  //  COLORE BADGE VERSIONE
  // ==========================
  let badgeClass = "version-badge-private";
  if (permessoTipo.includes("scrittura")) {
    badgeClass = "version-badge-write";
  } else if (permessoTipo.includes("lettura")) {
    badgeClass = "version-badge-read";
  }

  card.innerHTML = `
    <div class="card-header">
      <button class="note-menu-btn">⋮</button>
      <div class="version-badge-folder ${badgeClass}">v${versioneCorrente}</div>

      <div class="note-menu">
        <!-- Pulsante che cambia in base al ruolo dell'utente -->
      </div>
    </div>
    <h2>${nota.titolo}</h2>
    <p>${nota.contenuto}</p>
  `;

  // Aggiungi evento per aprire la nota in versione estesa (modale o sezione)
  card.addEventListener("click", () => {
    openNoteModal(nota);
  });

  // Aggiungi evento per eliminare la nota
  const menu = card.querySelector(".note-menu");
  const deleteBtn = document.createElement("button");

  // Se l'utente è l'autore, mostra il pulsante "Elimina nota"
  if (user === nota.creatore) {
    deleteBtn.textContent = "🗑 Elimina nota";
    deleteBtn.classList.add("delete-note-btn");    
  } else {
    // Se l'utente è in condivisione, mostra "Rimuoviti"
    deleteBtn.textContent = "⛓️‍💥 Rimuoviti ";
    deleteBtn.className = "remove-share-btn";
  }

  // Appendiamo il pulsante al menu
  menu.appendChild(deleteBtn);

  deleteBtn.addEventListener("click", async (e) => {
    e.stopPropagation();

    // Se è l'autore, elimina la nota
    if (user === nota.creatore) {
      // Controllo lock prima di chiedere conferma
      const lock = await getLockInfo(nota.id);
      if (lock && lock.lockedBy) {
        showToast("error", `❌ Nota in modifica da ${lock.lockedBy}. Eliminazione non consentita.`);
        return;
      }

      // Chiedi conferma
      showConfirmToast(
        `Vuoi davvero eliminare la nota "${nota.titolo}"?`,
        async () => {
          try {
            const res = await fetch(`${API_NOTES}/${nota.id}?user=${user}`, {
              method: "DELETE"
            });

            if (res.status === 403) {
              showToast("error", "❌ Non hai il permesso per eliminare questa nota.");
              return;
            }

            if (!res.ok) {
              showToast("error", "⚠️ Errore: impossibile eliminare la nota.");
              return;
            }

            showToast("success", "🗑 Nota eliminata!");
            caricaNote();

          } catch (err) {
            console.error("Errore:", err);
            showToast("error", "Errore durante l'eliminazione.");
          }
        }
      );
    } else {
      // Se è un utente condiviso, rimuovilo dalla condivisione
      showConfirmToast(
        `Vuoi davvero rimuovere la tua condivisione della nota "${nota.titolo}"?`,
        async () => {
          try {
            const res = await fetch(`${API_NOTES}/${nota.id}/removeSelf?user=${user}`, {
              method: "POST"
            });

            if (res.status === 403) {
              showToast("error", "❌ Non hai il permesso per rimuovere questa nota.");
              return;
            }

            if (!res.ok) {
              showToast("error", "⚠️ Errore: impossibile rimuovere la condivisione.");
              return;
            }

            showToast("success", "📤 Rimosso dalla condivisione!");
            caricaNote();

          } catch (err) {
            console.error("Errore:", err);
            showToast("error", "Errore durante la rimozione dalla condivisione.");
          }
        }
      );
    }
  });

  noteList.appendChild(card);

  // ---- MENU [⋮] ----
  const menuBtn = card.querySelector(".note-menu-btn");

  // apre/chiude il menu senza aprire la preview
  menuBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    menu.classList.toggle("open");
  });

  // chiudi menu cliccando fuori
  document.addEventListener("click", () => {
    menu.classList.remove("open");
  });

  
});
}


/* ===== FILTRO IN TEMPO REALE ===== */
searchInput.addEventListener("input", mostraNote);

/* ===== MODALE ANTEPRIMA — CHIUSURA ===== */
const previewModal = document.getElementById("previewModal");
const closePreviewBtn = document.getElementById("closePreviewBtn");


if (closePreviewBtn) {
  closePreviewBtn.addEventListener("click", () => {
    previewModal.style.display = "none";
  });
}

if (previewModal) {
  previewModal.addEventListener("click", (e) => {
    // Chiude cliccando fuori dalla card
    if (e.target === previewModal) {
      previewModal.style.display = "none";
    }
  });
}

/* ===== CREA NUOVA CARTELLA ===== */
const newFolderBtn = document.getElementById("newFolderBtn");

newFolderBtn.addEventListener("click", () => {
  showInputToast(
    "Nome della nuova cartella:",
    "Es. Università",
    (nome) => {
      window.location.href = `dashboard.html?newFolder=${encodeURIComponent(nome)}`;
      showToast("success", `📁 Cartella '${nome}' creata!`);
    },
    () => showToast("info", "Operazione annullata.")
  );
});

/* ===== FUNZIONE PER CAMBIARE COLORE ===== */
function lighten(hex, amount = 70) {
    if (!hex) return "#eee";
    let c = hex.replace("#", "").match(/.{1,2}/g);
    let r = Math.min(255, parseInt(c[0], 16) + amount).toString(16).padStart(2, "0");
    let g = Math.min(255, parseInt(c[1], 16) + amount).toString(16).padStart(2, "0");
    let b = Math.min(255, parseInt(c[2], 16) + amount).toString(16).padStart(2, "0");
    return `#${r}${g}${b}`;
}

function darken(hex, amount = 40) {
    if (!hex) return "#ccc";
    let c = hex.replace("#", "").match(/.{1,2}/g);
    let r = Math.max(0, parseInt(c[0], 16) - amount).toString(16).padStart(2, "0");
    let g = Math.max(0, parseInt(c[1], 16) - amount).toString(16).padStart(2, "0");
    let b = Math.max(0, parseInt(c[2], 16) - amount).toString(16).padStart(2, "0");
    return `#${r}${g}${b}`;
}

/* ===== AVVIO ===== */
caricaNote();