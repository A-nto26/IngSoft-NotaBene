// =====================================================
// 🌐 CONFIGURAZIONE BASE
// =====================================================
const API_VISIBLE_NOTES = "http://localhost:8080/api/notes/visible";
const API_NOTES = "http://localhost:8080/api/notes";
const user = (localStorage.getItem("loggedUser") || "").toLowerCase();

const folderList = document.getElementById("folderItems");
const noteList = document.getElementById("noteList");
const searchInput = document.getElementById("searchInput");
const noResults = document.getElementById("noResults");
const welcomeUser = document.getElementById("welcomeUser");

// Messaggio "Seleziona una cartella"
const noFolderSelected = document.getElementById("noFolderSelected");

// =====================================================
// 🟦 TOAST BASE
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
// 🟧 TOAST DI CONFERMA (SÌ / NO)
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
// 🟩 TOAST CON INPUT
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

// =====================================================
// CONTROLLI LOGIN
// =====================================================
if (!user) {
  showToast("error", "Devi effettuare il login.");
  setTimeout(() => window.location.replace("auth.html"), 1200);
}

if (welcomeUser) {
  welcomeUser.textContent = `Ciao, ${user}! 👋`;
}

// Logout
const logoutBtn = document.getElementById("logoutBtn");
if (logoutBtn) {
  logoutBtn.addEventListener("click", () => {
    localStorage.removeItem("loggedUser");
    window.location.replace("auth.html");
  });
}

// Torna alla dashboard
const backDashboardBtn = document.getElementById("backDashboardBtn");
if (backDashboardBtn) {
  backDashboardBtn.addEventListener("click", () => {
    window.location.href = "dashboard.html";
  });
}

let tutteLeNote = [];
let cartellaAttiva = null;

// =====================================================
// CARICA NOTE VISIBILI (Sprint 4)
// =====================================================
async function caricaNote() {
  try {
    const res = await fetch(`${API_VISIBLE_NOTES}/${encodeURIComponent(user)}`);
    tutteLeNote = await res.json();

    generaCartelle();
    mostraNote();
  } catch (err) {
    console.error("Errore caricamento note:", err);
  }
}

// =====================================================
// GENERA LISTA CARTELLE
// =====================================================
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

    const coloreCartella =
      tutteLeNote.find(n => n.cartella === nome)?.coloreCartella || "#cccccc";

    li.innerHTML = `
      <svg class="folder-icon-list" style="color:${coloreCartella};">
          <use href="#folder-fill"></use>
          <use href="#folder-stroke"></use>
      </svg>
      ${nome}
    `;

    li.dataset.colore = coloreCartella;

    li.addEventListener("click", () => {
      noFolderSelected.classList.add("hidden");

      document.querySelectorAll("#folderItems li").forEach(el => {
        el.classList.remove("active");
        el.style.backgroundColor = "";
        el.style.borderLeft = "";
      });

      li.classList.add("active");

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

// =====================================================
// MOSTRA NOTE IN CARTELLA
// =====================================================
function mostraNote() {
  noteList.innerHTML = "";

  if (!cartellaAttiva) {
    noFolderSelected.classList.remove("hidden");
    noResults.style.display = "none";
    return;
  }

  noFolderSelected.classList.add("hidden");

  const testoRicerca = searchInput.value.toLowerCase();
  let filtrate = tutteLeNote.filter(n => n.cartella === cartellaAttiva);

  if (testoRicerca) {
    filtrate = filtrate.filter(n =>
      n.titolo.toLowerCase().includes(testoRicerca) ||
      n.contenuto.toLowerCase().includes(testoRicerca)
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

    const permessoTipo = (nota.permesso?.tipo || "privata").toLowerCase();

    if (permessoTipo === "privata") card.classList.add("note-private");
    else if (permessoTipo.includes("lettura")) card.classList.add("shared-read");
    else if (permessoTipo.includes("scrittura")) card.classList.add("shared-write");

    const versioneCorrente = (nota.versioni?.length || 0) + 1;

    card.innerHTML = `
      <div class="card-header">
        <button class="note-menu-btn">⋮</button>
        <div class="version-badge-folder">v${versioneCorrente}</div>

        <div class="note-menu">
          <button class="delete-note-btn">🗑 Elimina nota</button>
        </div>
      </div>

      <h2>${nota.titolo}</h2>
      <p>${nota.contenuto}</p>
    `;

    // Preview
    card.addEventListener("click", () => {
      document.getElementById("previewTitolo").textContent = nota.titolo;
      document.getElementById("previewContenuto").textContent = nota.contenuto;
      document.getElementById("previewModal").style.display = "flex";
    });

    noteList.appendChild(card);

    // MENU ⋮
    const menuBtn = card.querySelector(".note-menu-btn");
    const menu = card.querySelector(".note-menu");
    const deleteBtn = card.querySelector(".delete-note-btn");

    menuBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      menu.classList.toggle("open");
    });

    document.addEventListener("click", () => {
      menu.classList.remove("open");
    });

    // ELIMINA NOTA
    deleteBtn.addEventListener("click", async (e) => {
      e.stopPropagation();

      showConfirmToast(
        `Vuoi davvero eliminare la nota "${nota.titolo}"?`,
        async () => {
          try {
            await fetch(`${API_NOTES}/${nota.id}?user=${user}`, {
              method: "DELETE"
            });

            showToast("success", "🗑 Nota eliminata!");
            caricaNote();

          } catch (err) {
            console.error("Errore eliminazione:", err);
            showToast("error", "Errore durante l'eliminazione.");
          }
        }
      );
    });
  });
}

// =====================================================
// FILTRO IN TEMPO REALE
// =====================================================
searchInput.addEventListener("input", mostraNote);

// =====================================================
// MODALE ANTEPRIMA
// =====================================================
const previewModal = document.getElementById("previewModal");
const closePreviewBtn = document.getElementById("closePreviewBtn");

if (closePreviewBtn) {
  closePreviewBtn.addEventListener("click", () => {
    previewModal.style.display = "none";
  });
}

if (previewModal) {
  previewModal.addEventListener("click", (e) => {
    if (e.target === previewModal) previewModal.style.display = "none";
  });
}

// =====================================================
// CREA NUOVA CARTELLA
// =====================================================
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

// =====================================================
// COLORI: lighten + darken
// =====================================================
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

// =====================================================
// AVVIO
// =====================================================
caricaNote();