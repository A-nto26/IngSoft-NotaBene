// =====================================================
// CONFIGURAZIONE BASE
// =====================================================
const API_NOTES = "http://localhost:8080/api/notes";
const API_USERS = "http://localhost:8080/api/users";

const noteList = document.getElementById("noteList");
const welcomeUser = document.getElementById("welcomeUser");
const user = localStorage.getItem("loggedUser");

// Stato globale
let editingNoteId = null;
let utentiSelezionati = [];
let showMie = true;
let showCondivise = true;

// =====================================================
//  SISTEMA TOAST (popup in alto a destra)
// =====================================================
function showToast(type, message) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.textContent = message;

  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 5000);
}

// =====================================================
//  BLOCCO ACCESSO DIRETTO
// =====================================================
if (!user) {
  showToast("error", "⚠️ Devi effettuare l'accesso per accedere alla dashboard.");
  window.location.replace("auth.html");
  throw new Error("Accesso non autorizzato: nessun utente loggato.");
} else if (welcomeUser) {
  welcomeUser.textContent = `Ciao, ${user}! 👋`;
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
//  HELPER: CARICA NOTE VISIBILI DA BACKEND (Sprint3)
// =====================================================
async function fetchVisibleNotes() {
  try {
    const res = await fetch(
      `${API_NOTES}/visibili/${encodeURIComponent(user)}`
    );
    if (!res.ok) {
      console.error("Errore caricamento note visibili:", res.status);
      return [];
    }
    const data = await res.json();
    return Array.isArray(data) ? data : [];
  } catch (e) {
    console.error("Errore rete fetchVisibleNotes:", e);
    return [];
  }
}

// Helper per ottenere una nota specifica (per le versioni)
async function fetchNotaById(id) {
  const notes = await fetchVisibleNotes();
  return notes.find((n) => n.id === id) || null;
}

// =====================================================
//  TOGGLE FILTRO NOTE (mostra mie / condivise)
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

// =====================================================
//  TOOLS PANEL – Apertura/Chiusura (+ / −)
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

// =====================================================
//  CARICAMENTO NOTE (Sprint3 – definitivo)
// =====================================================
async function caricaNote() {
  if (noteList) {
    noteList.innerHTML = `<div class="loading-notes">Caricamento note...</div>`;
  }

  const welcomeBox = document.getElementById("welcomeEmpty");
  const arrow = document.querySelector(".welcome-arrow");
  const searchArea = document.getElementById("searchArea");
  const side = document.getElementById("tutorialSide");
  const noResults = document.getElementById("noResults");

  const tutorialKey = `tutorialShown_${user}`;
  const tutorialShown = localStorage.getItem(tutorialKey) === "true";

  // Carica tutte le note visibili (mie + condivise) da backend
  const allNotes = await fetchVisibleNotes();
  const userLower = (user || "").trim().toLowerCase();

  // Caso 1 — UTENTE NUOVO (0 note totali)
  if (allNotes.length === 0) {
    if (noteList) noteList.innerHTML = "";
    if (welcomeBox) welcomeBox.classList.add("show");
    if (arrow) arrow.style.display = "block";
    if (searchArea) searchArea.style.display = "none";
    if (toolsPanel) toolsPanel.style.display = "none";
    if (filterRow) filterRow.style.display = "none";
    if (side) side.style.display = "none";
    document.body.classList.remove("tutorial-mode");
    if (noResults) noResults.style.display = "none";
    return;
  }

  // Da qui in poi l’utente ha almeno 1 nota
  if (welcomeBox) welcomeBox.classList.remove("show");
  if (arrow) arrow.style.display = "none";

  if (toolsPanel) toolsPanel.style.display = "block";
  if (searchArea) searchArea.style.display = "flex";
  if (filterRow) filterRow.style.display = "flex";

  // Caso 2 — UTENTE CON 1 SOLA NOTA E TUTORIAL NON ANCORA VISTO
  if (allNotes.length === 1 && !tutorialShown) {
    if (side) side.style.display = "block";
    document.body.classList.add("tutorial-mode");
  } else {
    if (side) side.style.display = "none";
    document.body.classList.remove("tutorial-mode");
  }

  //  Applica filtri "mie / condivise" lato frontend
  let notes = allNotes.filter((n) => {
    const autore =
      (n.creatore || "").trim().toLowerCase() === userLower;

    if (autore && showMie) return true;
    if (!autore && showCondivise) return true;
    return false;
  });

  if (!noteList) return;
  noteList.innerHTML = "";

  // Nessuna nota nella VISTA CORRENTE (filtri/ricerca)
  if (notes.length === 0) {
    if (noResults) noResults.style.display = "flex";
    return;
  } else {
    if (noResults) noResults.style.display = "none";
  }

  // Chiude eventuale modale nota aperta
  const noteModalEl = document.getElementById("noteModal");
  if (noteModalEl) noteModalEl.style.display = "none";

  // Ordina note per id desc
  notes.sort((a, b) => (b.id || 0) - (a.id || 0));

  // Render card
  notes.forEach((n) => {
    const isAutore =
      (n.creatore || "").trim().toLowerCase() === userLower;

    const permessoTipo = (n.permesso?.tipo || "Privata").toLowerCase();
    const versioneCorrente = (n.versioni?.length || 0) + 1;

    let badgePermesso = "🔒 Privata";
    if (permessoTipo.includes("scrittura")) badgePermesso = "🖊️ In scrittura";
    else if (permessoTipo.includes("lettura")) badgePermesso = "👓 In lettura";

    const card = document.createElement("div");
    if (isAutore) card.className = "note-card";
    else if (permessoTipo.includes("scrittura"))
      card.className = "note-card shared-write";
    else if (permessoTipo.includes("lettura"))
      card.className = "note-card shared-read";
    else card.className = "note-card shared";

    card.innerHTML = `
      <div class="version-badge">v${versioneCorrente}</div>
      <h2>${n.titolo}</h2>
      <p>${n.contenuto}</p>
      <p><small>📁 ${n.cartella || "—"}</small></p>
      <p><small>${badgePermesso}</small></p>
      <div class="actions">
        <button class="action-btn edit">✏️ Modifica</button>
        <button class="action-btn version">🕓 Versioni</button>
        <button class="action-btn delete">🗑️ Elimina</button>
      </div>
    `;

    const editBtn = card.querySelector(".edit");
    const deleteBtn = card.querySelector(".delete");
    const versionBtn = card.querySelector(".version");

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
          n.utentiCondivisi,
          n.permesso?.tipo,
          n.lastModifiedAt,
          n.lastModifiedBy,
          n.createdAt,
          n.creatore
        );
      });

      deleteBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        eliminaNota(n.id);
      });

      // Lettura (condivisa sola lettura)
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
          n.utentiCondivisi,
          n.permesso?.tipo,
          n.lastModifiedAt,
          n.lastModifiedBy,
          n.createdAt,
          n.creatore
        );
      });

      deleteBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        rimuovitiDallaNota(n.id);
      });

      // Scrittura (condivisa in scrittura)
    } else if (permessoTipo.includes("scrittura")) {
      editBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        apriModalModifica(
          n.id,
          n.titolo,
          n.contenuto,
          n.cartella,
          "scrittura",
          n.utentiCondivisi,
          n.permesso?.tipo,
          n.lastModifiedAt,
          n.lastModifiedBy,
          n.createdAt,
          n.creatore
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

    // Clic sulla card → anteprima in base al ruolo
    card.addEventListener("click", (e) => {
      if (e.target.closest(".action-btn")) return;

      const ruolo = isAutore
        ? "autore"
        : permessoTipo.includes("scrittura")
        ? "scrittura"
        : "lettura";

      apriModalAnteprima(
        n.id,
        n.titolo,
        n.contenuto,
        n.cartella,
        ruolo,
        n.utentiCondivisi,
        n.permesso?.tipo,
        n.lastModifiedAt,
        n.lastModifiedBy,
        n.createdAt,
        n.creatore
      );
    });

    noteList.appendChild(card);
  });
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

// =====================================================
//  CONTATORE CARATTERI
// =====================================================
const contenutoInput = document.getElementById("contenuto");
const charCount = document.getElementById("charCount");
const MAX_CHARS = 280;

if (contenutoInput && charCount) {
  contenutoInput.addEventListener("input", () => {
    let val = contenutoInput.value;
    if (val.length > MAX_CHARS) {
      val = val.slice(0, MAX_CHARS);
      contenutoInput.value = val;
    }
    const len = val.length;
    charCount.textContent = `${len}/${MAX_CHARS}`;
    charCount.style.color = "#999";
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
    const valore = permessoSelect.value; // già in UPPERCASE da HTML
    if (valore === "LETTURA" || valore === "SCRITTURA") {
      if (shareSection) shareSection.style.display = "block";
    } else {
      if (shareSection) shareSection.style.display = "none";
      utentiSelezionati = [];
      if (utentiSelezionatiDiv) utentiSelezionatiDiv.innerHTML = "";
    }
  });
}

function resetLockBanner() {
  const lockBanner = document.getElementById("lockBanner");
  if (lockBanner) {
    lockBanner.style.display = "none";
    lockBanner.textContent = "";
  }
}

function apriModalCrea() {
  editingNoteId = null;
  utentiSelezionati = [];
  document.querySelector("#duplicateBtn")?.remove();
  document.querySelector("#leaveShareBtn")?.remove();
  document.querySelector(".note-footer")?.remove();
  resetLockBanner();

  const titoloEl = document.getElementById("titolo");
  const cartellaEl = document.getElementById("cartella");
  const contenutoEl = document.getElementById("contenuto");

  if (!titoloEl || !cartellaEl || !contenutoEl || !permessoSelect) return;

  titoloEl.disabled = false;
  cartellaEl.disabled = false;
  contenutoEl.disabled = false;
  permessoSelect.disabled = false;

  titoloEl.value = "";
  cartellaEl.value = "";
  contenutoEl.value = "";
  permessoSelect.value = "PRIVATA";

  if (utentiSelezionatiDiv) utentiSelezionatiDiv.innerHTML = "";
  if (shareSection) shareSection.style.display = "none";

  modalTitle.textContent = "📝 CREA UNA NUOVA NOTA";
  saveNoteBtn.textContent = "💾 SALVA";
  saveNoteBtn.style.display = "inline-block";
  shareNoteBtn.style.display = "inline-block";
  cancelNoteBtn.style.display = "inline-block";

  const modalContent = document.querySelector(".modal-content");
  if (modalContent) {
    const guida = document.createElement("p");
    guida.className = "note-footer";
    guida.textContent = "✏️ Compila i campi per creare la tua nota.";
    modalContent.appendChild(guida);
  }

  if (charCount && contenutoInput) {
    contenutoInput.value = "";
    charCount.textContent = `0/${MAX_CHARS}`;
    charCount.style.color = "#999";
  }

  const arrow = document.querySelector(".welcome-arrow");
  if (arrow) arrow.style.display = "none";

  if (modal) modal.style.display = "flex";
}

function apriModalAnteprima(
  id,
  titolo,
  contenuto,
  cartella,
  ruolo = "autore",
  utentiCondivisi = [],
  permessoTipo = "Privata",
  lastModifiedAt = null,
  lastModifiedBy = null,
  createdAt = null,
  creatore = null
) {
  editingNoteId = id;

  const titoloEl = document.getElementById("titolo");
  const cartellaEl = document.getElementById("cartella");
  const contenutoEl = document.getElementById("contenuto");
  const modalActions = document.querySelector(".modal-actions");
  const modalContent = document.querySelector(".modal-content");

  if (!titoloEl || !cartellaEl || !contenutoEl || !modalActions || !modalContent)
    return;

  document.querySelector("#duplicateBtn")?.remove();
  document.querySelector("#leaveShareBtn")?.remove();
  document.querySelector(".note-footer")?.remove();
  resetLockBanner();

  titoloEl.value = titolo;
  cartellaEl.value = cartella || "";
  contenutoEl.value = contenuto;

  if (utentiSelezionatiDiv) {
    utentiSelezionatiDiv.innerHTML =
      utentiCondivisi && utentiCondivisi.length > 0
        ? "👥 Utenti selezionati: " + utentiCondivisi.join(", ")
        : "";
  }

  if (permessoSelect) {
    permessoSelect.value = (permessoTipo || "Privata").toUpperCase();
    permessoSelect.disabled = true;
  }

  titoloEl.disabled = true;
  cartellaEl.disabled = true;
  contenutoEl.disabled = true;

  shareNoteBtn.style.display = "none";
  saveNoteBtn.style.display = "none";
  cancelNoteBtn.style.display = "inline-block";

  modalTitle.textContent = "👁️ Anteprima nota";

  const duplicaBtn = document.createElement("button");
  duplicaBtn.id = "duplicateBtn";
  duplicaBtn.className = "save-btn";
  duplicaBtn.textContent = "📄 DUPLICA";
  duplicaBtn.style.marginLeft = "0.5rem";
  duplicaBtn.onclick = () => duplicaNota(id);
  modalActions.appendChild(duplicaBtn);

  // Bottone "Rimuovimi" solo se NON sei autore
  if (ruolo !== "autore") {
    const leaveBtn = document.createElement("button");
    leaveBtn.id = "leaveShareBtn";
    leaveBtn.className = "leave-btn";
    leaveBtn.textContent = "👋 Rimuovimi";
    leaveBtn.onclick = () => rimuovitiDallaNota(id);
    modalActions.appendChild(leaveBtn);
  }

  const footer = document.createElement("p");
  footer.className = "note-footer";
  const autore = lastModifiedBy || creatore || "autore sconosciuto";

  if (lastModifiedAt) {
    footer.textContent = `Ultima modifica – ${new Date(
      lastModifiedAt
    ).toLocaleString("it-IT")} (${autore})`;
  } else if (createdAt) {
    footer.textContent = `Creata il ${new Date(
      createdAt
    ).toLocaleString("it-IT")} (${creatore || "autore sconosciuto"})`;
  } else {
    footer.textContent = `Creata da ${creatore || "autore sconosciuto"}`;
  }

  modalContent.appendChild(footer);

  if (modal) modal.style.display = "flex";
}

// Modifica (Sprint3, senza lock lato frontend)
function apriModalModifica(
  id,
  titolo,
  contenuto,
  cartella,
  ruolo = "autore",
  utentiCondivisi = [],
  permessoTipo = "Privata",
  lastModifiedAt = null,
  lastModifiedBy = null,
  createdAt = null,
  creatore = null
) {
  editingNoteId = id;
  utentiSelezionati = utentiCondivisi || [];

  const titoloEl = document.getElementById("titolo");
  const cartellaEl = document.getElementById("cartella");
  const contenutoEl = document.getElementById("contenuto");
  const modalContent = document.querySelector(".modal-content");
  const modalActions = document.querySelector(".modal-actions");

  if (!titoloEl || !cartellaEl || !contenutoEl || !modalContent || !modalActions) return;

  document.querySelector("#duplicateBtn")?.remove();
  document.querySelector("#leaveShareBtn")?.remove();
  document.querySelector(".note-footer")?.remove();
  resetLockBanner();

  titoloEl.value = titolo;
  cartellaEl.value = cartella || "";
  contenutoEl.value = contenuto;

  if (utentiSelezionatiDiv) {
    utentiSelezionatiDiv.innerHTML = utentiSelezionati.length
      ? "👥 Utenti selezionati: " + utentiSelezionati.join(", ")
      : "";
  }

  if (permessoSelect) {
    permessoSelect.value = (permessoTipo || "Privata").toUpperCase();
    permessoSelect.disabled = true; // non modificabile dopo creazione
  }

  if (ruolo === "autore") {
    modalTitle.textContent = "✏️ Modifica nota";
    saveNoteBtn.textContent = "💾 Aggiorna";
    shareNoteBtn.style.display = "inline-block";
    saveNoteBtn.style.display = "inline-block";
    cancelNoteBtn.style.display = "inline-block";
    titoloEl.disabled = false;
    cartellaEl.disabled = false;
    contenutoEl.disabled = false;
  } else if ((permessoTipo || "").toLowerCase().includes("lettura")) {
    modalTitle.textContent = "👁️ Anteprima nota (sola lettura)";
    titoloEl.disabled = true;
    cartellaEl.disabled = true;
    contenutoEl.disabled = true;
    shareNoteBtn.style.display = "none";
    saveNoteBtn.style.display = "none";
    cancelNoteBtn.style.display = "inline-block";

    const leaveBtn = document.createElement("button");
    leaveBtn.id = "leaveShareBtn";
    leaveBtn.className = "leave-btn";
    leaveBtn.textContent = "👋 Rimuovimi";
    leaveBtn.onclick = () => rimuovitiDallaNota(id);
    modalActions.appendChild(leaveBtn);
  } else {
    // scrittura ma non autore
    modalTitle.textContent = "🖊️ Modifica nota condivisa";
    saveNoteBtn.textContent = "💾 Aggiorna";
    shareNoteBtn.style.display = "none"; // solo autore condivide
    saveNoteBtn.style.display = "inline-block";
    cancelNoteBtn.style.display = "inline-block";
    titoloEl.disabled = false;
    cartellaEl.disabled = false;
    contenutoEl.disabled = false;

    const leaveBtn = document.createElement("button");
    leaveBtn.id = "leaveShareBtn";
    leaveBtn.className = "leave-btn";
    leaveBtn.textContent = "👋 Rimuovimi";
    leaveBtn.onclick = () => rimuovitiDallaNota(id);
    modalActions.appendChild(leaveBtn);
  }

  const footer = document.createElement("p");
  footer.className = "note-footer";
  const autore = lastModifiedBy || creatore || "autore sconosciuto";

  if (lastModifiedAt) {
    footer.textContent = `Ultima modifica – ${new Date(
      lastModifiedAt
    ).toLocaleString("it-IT")} (${autore})`;
  } else if (createdAt) {
    footer.textContent = `Creata il ${new Date(
      createdAt
    ).toLocaleString("it-IT")} (${creatore || "autore sconosciuto"})`;
  } else {
    footer.textContent = `Creata da ${creatore || "autore sconosciuto"}`;
  }

  modalContent.appendChild(footer);

  const arrow = document.querySelector(".welcome-arrow");
  if (arrow) arrow.style.display = "none";

  if (modal) modal.style.display = "flex";
}

function chiudiModal(id) {
  const modalEl = document.getElementById(id);
  if (modalEl) modalEl.style.display = "none";

  if (id === "noteModal") {
    editingNoteId = null;

    if (charCount) {
      charCount.textContent = `0/${MAX_CHARS}`;
      charCount.style.color = "#999";
    }

    const arrow = document.querySelector(".welcome-arrow");
    if (arrow) {
      fetchVisibleNotes().then((allNotes) => {
        if (allNotes.length === 0) {
          arrow.style.display = "block";
        } else {
          arrow.style.display = "none";
        }
      });
    }
  }
}

// =====================================================
//  SALVATAGGIO NOTE (Create + Update)
// =====================================================
if (saveNoteBtn) {
  saveNoteBtn.addEventListener("click", async () => {
    const titoloEl = document.getElementById("titolo");
    const contenutoEl = document.getElementById("contenuto");
    const cartellaEl = document.getElementById("cartella");

    if (!titoloEl || !contenutoEl || !cartellaEl) return;

    const titolo = titoloEl.value.trim();
    const contenuto = contenutoEl.value.trim();
    const cartella = cartellaEl.value.trim();

    let permesso = permessoSelect?.value || "PRIVATA";
    permesso = permesso.toUpperCase(); // backend: PRIVATA | LETTURA | SCRITTURA

    if (!titolo || !contenuto) {
      showToast("info", "⚠️ Inserisci titolo e contenuto!");
      return;
    }

    const isUpdate = !!editingNoteId;
    let body;

    if (isUpdate) {
      // NoteUpdateRequest (Sprint3: titolo, contenuto, cartella)
      body = {
        titolo,
        contenuto,
        cartella: cartella || null
      };
    } else {
      // CreateNoteRequest
      body = {
        titolo,
        contenuto,
        creatore: user,
        cartella: cartella || null,
        utentiCondivisi: utentiSelezionati,
        permesso
      };
    }

    const method = isUpdate ? "PUT" : "POST";
    const url = isUpdate
      ? `${API_NOTES}/${Number(editingNoteId)}?user=${encodeURIComponent(user)}`
      : API_NOTES;

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });

      if (res.ok) {
        showToast(
          "success",
          isUpdate ? "✏️ Nota aggiornata!" : "✅ Nota creata con successo!"
        );
      } else {
        const msg = await res.text();
        console.error("Errore backend:", msg);
        showToast(
          "error",
          `Errore durante il salvataggio della nota (${res.status})`
        );
      }
    } catch (err) {
      console.error("Errore di rete:", err);
      showToast("error", "Errore di connessione al server.");
    } finally {
      chiudiModal("noteModal");
      await caricaNote();
    }
  });
}

// =====================================================
// ELIMINAZIONE NOTE
// =====================================================
async function eliminaNota(id) {
  if (!confirm("Vuoi davvero eliminare questa nota?")) return;

  try {
    const res = await fetch(
      `${API_NOTES}/${id}?user=${encodeURIComponent(user)}`,
      { method: "DELETE" }
    );

    if (res.ok) {
      showToast("success", "🗑️ Nota eliminata!");
      await caricaNote();
    } else if (res.status === 403) {
      showToast("error", "❌ Solo l'autore può eliminare questa nota.");
    } else {
      showToast("error", "❌ Errore durante l'eliminazione della nota.");
    }
  } catch (err) {
    console.error("Errore eliminazione nota:", err);
    showToast("error", "Errore di connessione durante l'eliminazione.");
  }
}

// Rimuovere se stessi da una nota condivisa
async function rimuovitiDallaNota(id) {
  if (!confirm("Vuoi rimuovere questa nota dalla tua vista?")) return;

  try {
    const res = await fetch(
      `${API_NOTES}/${id}/removeSelf`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: user })
      }
    );

    if (res.ok) {
      showToast("success", "👋 Ti sei rimosso dalla nota condivisa.");
      chiudiModal("noteModal");
      await caricaNote();
    } else {
      showToast("error", "❌ Errore durante la rimozione dalla nota condivisa.");
    }
  } catch (err) {
    console.error("Errore rimozione nota condivisa:", err);
    showToast("error", "Errore di connessione durante la rimozione.");
  }
}

// =====================================================
//  VERSIONI
// =====================================================
async function mostraVersioni(id) {
  const versionModal = document.getElementById("versionModal");
  const versionListModal = document.getElementById("versionListModal");
  if (!versionModal || !versionListModal) return;

  versionListModal.innerHTML = "<p>Caricamento versioni...</p>";
  versionModal.style.display = "flex";

  try {
    const nota = await fetchNotaById(id);
    if (!nota) {
      versionListModal.innerHTML = "<p>Nota non trovata.</p>";
      return;
    }

    if (!nota.versioni || nota.versioni.length === 0) {
      versionListModal.innerHTML = "<p>Nessuna versione salvata.</p>";
      return;
    }

    versionListModal.innerHTML = "";
    nota.versioni.forEach((v, i) => {
      const timeLabel = tempoRelativo(v.timestamp);
      const div = document.createElement("div");
      div.className = "version-item";
      div.innerHTML = `
          <small>Versione #${i + 1} — ${timeLabel}</small>
          <h3>📝 ${v.titolo || "(Titolo mancante)"}</h3>
          <p>${v.contenuto || "(Contenuto vuoto)"}</p>
          <button class="restore-btn">Ripristina</button>
      `;
      div
        .querySelector(".restore-btn")
        .addEventListener("click", () => ripristinaVersione(id, i));
      versionListModal.appendChild(div);
    });
  } catch (err) {
    console.error("Errore caricamento versioni:", err);
    versionListModal.innerHTML = "<p>Errore di connessione.</p>";
  }
}

const closeVersionBtn = document.getElementById("closeVersionBtn");
if (closeVersionBtn) {
  closeVersionBtn.addEventListener("click", () => chiudiModal("versionModal"));
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
  if (!confirm(`Vuoi ripristinare la versione #${index + 1}?`)) return;

  try {
    const res = await fetch(
      `${API_NOTES}/${id}/restore/${index}?user=${encodeURIComponent(user)}`,
      { method: "POST" }
    );

    if (!res.ok) {
      if (res.status === 403) {
        showToast("error", "❌ Non hai i permessi per ripristinare questa versione.");
      } else {
        showToast("error", "❌ Errore nel ripristino della versione.");
      }
      return;
    }

    showToast("success", "🔙 Versione ripristinata!");
    chiudiModal("versionModal");

    // Ricarica TUTTE le note (dopo che il backend ha aggiornato)
    await caricaNote();

    // Recupera la nota aggiornata (evitando cache interna)
    const resNota = await fetch(`${API_NOTES}/visibili/${user}`);
    const tutte = await resNota.json();
    const notaAgg = tutte.find(n => n.id === id);

    if (notaAgg) {
      // Riapri l’anteprima con la versione aggiornata
      apriModalAnteprima(
        notaAgg.id,
        notaAgg.titolo,
        notaAgg.contenuto,
        notaAgg.cartella,
        (notaAgg.creatore === user ? "autore" : "scrittura"),
        notaAgg.utentiCondivisi,
        notaAgg.permesso?.tipo,
        notaAgg.lastModifiedAt,
        notaAgg.lastModifiedBy,
        notaAgg.createdAt,
        notaAgg.creatore
      );
    }

  } catch (err) {
    console.error("Errore ripristino versione:", err);
    showToast("error", "Errore di connessione durante il ripristino.");
  }
}

// =====================================================
//  CONDIVISIONE NOTE (modal selezione utenti)
// =====================================================
const shareModal = document.getElementById("shareModal");
const userListShare = document.getElementById("userListShare");
const shareSaveBtn = document.getElementById("shareSaveBtn");
const shareCancelBtn = document.getElementById("shareCancelBtn");

if (shareNoteBtn) {
  shareNoteBtn.addEventListener("click", async () => {
    if (!shareModal || !userListShare) return;

    shareModal.style.display = "flex";
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
        if (nome !== user) {
          const riga = document.createElement("div");
          riga.className = "user-item";

          const cb = document.createElement("input");
          cb.type = "checkbox";
          cb.value = nome;
          cb.style.display = "none";

          const pill = document.createElement("div");
          pill.className = "user-pill";
          pill.textContent = nome;

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
        }
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

  aggiornaListaUtenti();
}

function aggiornaListaUtenti() {
  if (utentiSelezionatiDiv) {
    utentiSelezionatiDiv.innerHTML =
      utentiSelezionati.length === 0
        ? ""
        : "👥 Utenti selezionati: " + utentiSelezionati.join(", ");
  }
}

if (shareCancelBtn) {
  shareCancelBtn.addEventListener("click", () => chiudiModal("shareModal"));
}

// ShareSave: se sto creando una nota nuova → solo aggiorna lista;
// se sto modificando una nota esistente (editingNoteId) → chiama API /share
if (shareSaveBtn) {
  shareSaveBtn.addEventListener("click", async () => {
    aggiornaListaUtenti();

    // se NON c'è una nota già esistente, siamo in "creazione" → niente API share
    if (!editingNoteId) {
      chiudiModal("shareModal");
      return;
    }

    try {
      const res = await fetch(
        `${API_NOTES}/${editingNoteId}/share?user=${encodeURIComponent(user)}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            utentiCondivisi: utentiSelezionati
          })
        }
      );

      if (res.ok) {
        showToast("success", "🤝 Nota condivisa/aggiornata con successo.");
      } else {
        showToast("error", "❌ Errore durante l'aggiornamento della condivisione.");
      }
    } catch (err) {
      console.error("Errore condivisione:", err);
      showToast("error", "Errore di connessione durante la condivisione.");
    } finally {
      chiudiModal("shareModal");
      await caricaNote();
    }
  });
}

// =====================================================
//  FILTRO NOTE IN TEMPO REALE (solo lato frontend)
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
//  DUPLICA NOTA
// =====================================================
async function duplicaNota(id) {
  try {
    const res = await fetch(
      `${API_NOTES}/${id}/duplicate?creatore=${encodeURIComponent(user)}`,
      { method: "POST" }
    );

    if (!res.ok) throw new Error("Errore duplicazione");

    showToast("success", "📄 Nota duplicata con successo!");
    editingNoteId = null;
  } catch (err) {
    console.error("Errore duplicazione:", err);
    showToast("error", "❌ Errore durante la duplicazione della nota.");
  } finally {
    chiudiModal("noteModal");
    await caricaNote();
  }
}

// =====================================================
//  MINI TUTORIAL – Pulsante "Ho capito"
// =====================================================
const tutorialSideBtn = document.getElementById("tutorialSideBtn");
if (tutorialSideBtn) {
  tutorialSideBtn.addEventListener("click", () => {
    localStorage.setItem(`tutorialShown_${user}`, "true");

    const side = document.getElementById("tutorialSide");
    if (side) side.style.display = "none";

    document.body.classList.remove("tutorial-mode");

    document.querySelectorAll(".note-card").forEach((card) => {
      card.style.opacity = "1";
    });

    aggiornaToolsPanel();
    caricaNote();
  });
}

// =====================================================
// AVVIO INIZIALE
// =====================================================
caricaNote();
