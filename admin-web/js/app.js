/**
 * Quiz App Admin Portal - Interactive Logic
 * Fully synchronized with Firebase Firestore & Auth
 * Screen Flow: Login Screen First -> Admin Dashboard
 */

// Application State
const state = {
  questions: [],
  filteredQuestions: [],
  students: [],
  currentAdmin: null,
  isDemoMode: false,
  deleteTargetId: null,
  parsedImportQuestions: [],
  isListenersInitialized: false,
  filters: {
    search: '',
    subject: 'ALL',
    difficulty: 'ALL',
    status: 'ALL'
  }
};

// DOM Elements
const elements = {
  // Views
  loginScreenView: document.getElementById('loginScreenView'),
  dashboardView: document.getElementById('dashboardView'),

  // Dedicated Login Form
  dedicatedLoginForm: document.getElementById('dedicatedLoginForm'),
  loginEmail: document.getElementById('loginEmail'),
  loginPassword: document.getElementById('loginPassword'),
  oneClickDemoBtn: document.getElementById('oneClickDemoBtn'),
  loginSubmitBtn: document.getElementById('loginSubmitBtn'),

  // Top Nav
  connectionDot: document.getElementById('connectionDot'),
  connectionText: document.getElementById('connectionText'),
  userBadge: document.getElementById('userBadge'),
  userAvatar: document.getElementById('userAvatar'),
  userEmail: document.getElementById('userEmail'),
  logoutBtn: document.getElementById('logoutBtn'),
  themeToggleBtn: document.getElementById('themeToggleBtn'),

  // Stats
  statTotalQuestions: document.getElementById('statTotalQuestions'),
  statActiveQuestions: document.getElementById('statActiveQuestions'),
  statTotalSubjects: document.getElementById('statTotalSubjects'),
  statTotalStudents: document.getElementById('statTotalStudents'),

  // Search & Filter
  searchInput: document.getElementById('searchInput'),
  filterSubject: document.getElementById('filterSubject'),
  filterDifficulty: document.getElementById('filterDifficulty'),
  filterStatus: document.getElementById('filterStatus'),
  filteredCountBadge: document.getElementById('filteredCountBadge'),

  // Action Buttons
  addQuestionBtn: document.getElementById('addQuestionBtn'),
  importJsonBtn: document.getElementById('importJsonBtn'),
  exportJsonBtn: document.getElementById('exportJsonBtn'),
  viewLeaderboardBtn: document.getElementById('viewLeaderboardBtn'),
  emptyAddBtn: document.getElementById('emptyAddBtn'),

  // Table & Empty
  questionsTable: document.getElementById('questionsTable'),
  questionsTableBody: document.getElementById('questionsTableBody'),
  emptyState: document.getElementById('emptyState'),

  // Modals
  questionModal: document.getElementById('questionModal'),
  questionForm: document.getElementById('questionForm'),
  modalTitle: document.getElementById('modalTitle'),
  editDocId: document.getElementById('editDocId'),
  formSubject: document.getElementById('formSubject'),
  formDifficulty: document.getElementById('formDifficulty'),
  formText: document.getElementById('formText'),
  formOpt0: document.getElementById('formOpt0'),
  formOpt1: document.getElementById('formOpt1'),
  formOpt2: document.getElementById('formOpt2'),
  formOpt3: document.getElementById('formOpt3'),
  formActive: document.getElementById('formActive'),

  importModal: document.getElementById('importModal'),
  dropZone: document.getElementById('dropZone'),
  jsonFileInput: document.getElementById('jsonFileInput'),
  loadSampleJsonBtn: document.getElementById('loadSampleJsonBtn'),
  importPreview: document.getElementById('importPreview'),
  previewCountBadge: document.getElementById('previewCountBadge'),
  previewSample: document.getElementById('previewSample'),
  importProgressWrapper: document.getElementById('importProgressWrapper'),
  importProgressBar: document.getElementById('importProgressBar'),
  importProgressPercent: document.getElementById('importProgressPercent'),
  importProgressStatus: document.getElementById('importProgressStatus'),
  startImportBtn: document.getElementById('startImportBtn'),

  deleteModal: document.getElementById('deleteModal'),
  deleteQuestionStatement: document.getElementById('deleteQuestionStatement'),
  confirmDeleteBtn: document.getElementById('confirmDeleteBtn'),

  leaderboardModal: document.getElementById('leaderboardModal'),
  leaderboardTableBody: document.getElementById('leaderboardTableBody'),
  leaderboardEmpty: document.getElementById('leaderboardEmpty'),

  toastContainer: document.getElementById('toastContainer')
};

// ==================== INITIALIZATION ====================

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  setupEventListeners();
  checkAuthSession();
});

// Theme Management
function initTheme() {
  const savedTheme = localStorage.getItem('quiz_admin_theme') || 'dark';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeIcon(savedTheme);
}

function updateThemeIcon(theme) {
  const icon = elements.themeToggleBtn.querySelector('i');
  if (theme === 'light') {
    icon.className = 'fa-solid fa-sun';
  } else {
    icon.className = 'fa-solid fa-moon';
  }
}

elements.themeToggleBtn.addEventListener('click', () => {
  const current = document.documentElement.getAttribute('data-theme') || 'dark';
  const newTheme = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', newTheme);
  localStorage.setItem('quiz_admin_theme', newTheme);
  updateThemeIcon(newTheme);
});

// Toast Notifications
function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  let icon = 'fa-circle-info';
  if (type === 'success') icon = 'fa-circle-check';
  if (type === 'error') icon = 'fa-triangle-exclamation';

  toast.innerHTML = `<i class="fa-solid ${icon}"></i><span>${message}</span>`;
  elements.toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// ==================== AUTHENTICATION & VIEW SWITCHING ====================

function showLoginScreen() {
  elements.loginScreenView.style.display = 'flex';
  elements.dashboardView.style.display = 'none';
}

function showDashboardScreen(email, isDemo = false) {
  elements.loginScreenView.style.display = 'none';
  elements.dashboardView.style.display = 'block';

  elements.userAvatar.textContent = email.charAt(0).toUpperCase();
  elements.userEmail.textContent = email + (isDemo ? ' (Demo)' : '');

  // Initialize listeners once dashboard opens
  if (!state.isListenersInitialized) {
    initFirebaseListeners();
    state.isListenersInitialized = true;
  }
}

function checkAuthSession() {
  const isDemo = localStorage.getItem('quiz_demo_admin') === 'true';
  if (isDemo) {
    state.isDemoMode = true;
    showDashboardScreen('evaluator.admin@quiz.com', true);
    return;
  }

  if (auth) {
    auth.onAuthStateChanged((user) => {
      if (user) {
        state.currentAdmin = user;
        showDashboardScreen(user.email, false);
      } else {
        showLoginScreen();
      }
    });
  } else {
    showLoginScreen();
  }
}

// Dedicated Login Form Submit
elements.dedicatedLoginForm.addEventListener('submit', async (e) => {
  e.preventDefault();

  const email = elements.loginEmail.value.trim();
  const password = elements.loginPassword.value.trim();

  if (!email || !password) {
    showToast('Please enter both email and password', 'error');
    return;
  }

  elements.loginSubmitBtn.disabled = true;
  elements.loginSubmitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i><span>Signing In...</span>';

  try {
    if (auth) {
      const cred = await auth.signInWithEmailAndPassword(email, password);
      const user = cred.user;

      // Verify admin role in Firestore
      if (db) {
        const doc = await db.collection('roles').document(user.uid).get();
        if (!doc.exists || doc.data().role !== 'admin') {
          await auth.signOut();
          showToast('Access Denied: This account is not an Administrator.', 'error');
          elements.loginSubmitBtn.disabled = false;
          elements.loginSubmitBtn.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i><span>Sign In to Dashboard</span>';
          return;
        }
      }

      state.currentAdmin = user;
      showToast('Welcome back, Admin!', 'success');
      showDashboardScreen(user.email, false);
    } else {
      // Local demo fallback if Firebase not loaded
      state.isDemoMode = true;
      localStorage.setItem('quiz_demo_admin', 'true');
      showToast('Signed in via demo mode (Firebase offline)', 'info');
      showDashboardScreen(email, true);
    }
  } catch (error) {
    console.error('Login error:', error);
    showToast('Login Failed: ' + error.message, 'error');
  } finally {
    elements.loginSubmitBtn.disabled = false;
    elements.loginSubmitBtn.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i><span>Sign In to Dashboard</span>';
  }
});

// One-Click Demo Mode Button
elements.oneClickDemoBtn.addEventListener('click', () => {
  state.isDemoMode = true;
  localStorage.setItem('quiz_demo_admin', 'true');
  showToast('Authenticated in Evaluator Demo Mode!', 'success');
  showDashboardScreen('evaluator.admin@quiz.com', true);
});

// Logout Button
elements.logoutBtn.addEventListener('click', () => {
  if (auth && !state.isDemoMode) {
    auth.signOut();
  }
  state.currentAdmin = null;
  state.isDemoMode = false;
  localStorage.removeItem('quiz_demo_admin');
  showToast('Logged out of Admin Portal', 'info');
  showLoginScreen();
});

// ==================== FIREBASE SYNC ====================

function initFirebaseListeners() {
  if (!db) {
    elements.connectionDot.className = 'status-dot';
    elements.connectionText.textContent = 'Offline / Demo';
    showToast('Firebase not loaded. Running in local preview mode.', 'info');
    loadFallbackData();
    return;
  }

  // Real-time listener for questions
  elements.connectionText.textContent = 'Syncing...';
  db.collection('questions').onSnapshot(
    (snapshot) => {
      elements.connectionDot.className = 'status-dot connected';
      elements.connectionText.textContent = 'Live Connected';

      const questions = [];
      snapshot.forEach((doc) => {
        const data = doc.data();
        questions.push({
          id: doc.id,
          text: data.text || '',
          subject: data.subject || 'DSA',
          difficulty: data.difficulty || 'Easy',
          options: Array.isArray(data.options) ? data.options : [],
          answerIndex: typeof data.answerIndex === 'number' ? data.answerIndex : 0,
          active: data.active !== false
        });
      });

      state.questions = questions;
      applyFilters();
      updateDashboardStats();
    },
    (error) => {
      console.error('Firestore questions snapshot error:', error);
      elements.connectionDot.className = 'status-dot';
      elements.connectionText.textContent = 'Offline / Rules Protected';
      if (state.questions.length === 0) {
        loadFallbackData();
      }
    }
  );

  // Real-time listener for roles / students
  db.collection('roles').onSnapshot(
    (snapshot) => {
      const students = [];
      snapshot.forEach((doc) => {
        const data = doc.data();
        students.push({
          id: doc.id,
          email: data.email || 'Anonymous',
          role: data.role || 'student',
          score: data.score || 0
        });
      });
      state.students = students;
      elements.statTotalStudents.textContent = students.length;
    },
    (err) => console.warn('Roles listener restricted:', err)
  );
}

// Update Dashboard Numbers
function updateDashboardStats() {
  elements.statTotalQuestions.textContent = state.questions.length;
  
  const activeCount = state.questions.filter(q => q.active).length;
  elements.statActiveQuestions.textContent = activeCount;

  const subjects = new Set(state.questions.map(q => q.subject));
  elements.statTotalSubjects.textContent = Math.max(subjects.size, 4);
}

// ==================== FILTERING & SEARCH ====================

function applyFilters() {
  const { search, subject, difficulty, status } = state.filters;
  const query = search.trim().toLowerCase();

  state.filteredQuestions = state.questions.filter((q) => {
    // Subject filter
    if (subject !== 'ALL' && q.subject.toLowerCase() !== subject.toLowerCase()) {
      return false;
    }
    // Difficulty filter
    if (difficulty !== 'ALL' && q.difficulty.toLowerCase() !== difficulty.toLowerCase()) {
      return false;
    }
    // Status filter
    if (status === 'ACTIVE' && !q.active) return false;
    if (status === 'INACTIVE' && q.active) return false;

    // Search query
    if (query) {
      const matchText = q.text.toLowerCase().includes(query);
      const matchOption = q.options.some(opt => opt.toLowerCase().includes(query));
      if (!matchText && !matchOption) return false;
    }

    return true;
  });

  renderTable();
}

function renderTable() {
  const tbody = elements.questionsTableBody;
  tbody.innerHTML = '';
  elements.filteredCountBadge.textContent = `${state.filteredQuestions.length} questions`;

  if (state.filteredQuestions.length === 0) {
    elements.questionsTable.style.display = 'none';
    elements.emptyState.style.display = 'block';
    return;
  }

  elements.questionsTable.style.display = 'table';
  elements.emptyState.style.display = 'none';

  state.filteredQuestions.forEach((q) => {
    const tr = document.createElement('tr');

    // Subject Badge
    const subClass = getSubjectBadgeClass(q.subject);
    const diffClass = getDifficultyBadgeClass(q.difficulty);

    // Options markup with correct highlight
    const optionsHtml = q.options.map((opt, idx) => {
      const isCorrect = idx === q.answerIndex;
      return `
        <div class="opt-pill ${isCorrect ? 'correct' : ''}">
          <i class="fa-solid ${isCorrect ? 'fa-circle-check' : 'fa-circle-dot'}"></i>
          <span>${escapeHtml(opt)}</span>
        </div>
      `;
    }).join('');

    tr.innerHTML = `
      <td>
        <span class="badge ${subClass}">${escapeHtml(q.subject)}</span>
      </td>
      <td>
        <span class="badge ${diffClass}">${escapeHtml(q.difficulty)}</span>
      </td>
      <td class="question-cell">
        <div class="question-text">${escapeHtml(q.text)}</div>
        <div class="options-preview">
          ${optionsHtml}
        </div>
      </td>
      <td style="text-align: center;">
        <label class="toggle-switch">
          <input type="checkbox" ${q.active ? 'checked' : ''} onchange="toggleQuestionActive('${q.id}', this.checked)">
          <span class="toggle-slider"></span>
        </label>
      </td>
      <td style="text-align: right;">
        <button class="btn-icon-only" onclick="openEditModal('${q.id}')" title="Edit Question" style="margin-right: 6px;">
          <i class="fa-solid fa-pen-to-square"></i>
        </button>
        <button class="btn-icon-only" onclick="openDeleteModal('${q.id}')" title="Delete Question" style="color: var(--danger);">
          <i class="fa-solid fa-trash-can"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function getSubjectBadgeClass(subject) {
  const s = (subject || '').toUpperCase();
  if (s === 'DSA') return 'badge-subject-dsa';
  if (s === 'DBMS') return 'badge-subject-dbms';
  if (s === 'OS') return 'badge-subject-os';
  if (s === 'NETWORKS') return 'badge-subject-networks';
  return 'badge-subject-other';
}

function getDifficultyBadgeClass(difficulty) {
  const d = (difficulty || '').toLowerCase();
  if (d === 'easy') return 'badge-diff-easy';
  if (d === 'hard') return 'badge-diff-hard';
  return 'badge-diff-medium';
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// ==================== EVENT LISTENERS ====================

function setupEventListeners() {
  // Search
  elements.searchInput.addEventListener('input', (e) => {
    state.filters.search = e.target.value;
    applyFilters();
  });

  // Filters
  elements.filterSubject.addEventListener('change', (e) => {
    state.filters.subject = e.target.value;
    applyFilters();
  });

  elements.filterDifficulty.addEventListener('change', (e) => {
    state.filters.difficulty = e.target.value;
    applyFilters();
  });

  elements.filterStatus.addEventListener('change', (e) => {
    state.filters.status = e.target.value;
    applyFilters();
  });

  // Action Buttons
  elements.addQuestionBtn.addEventListener('click', openAddModal);
  elements.emptyAddBtn.addEventListener('click', openAddModal);
  elements.importJsonBtn.addEventListener('click', () => openModal(elements.importModal));
  elements.exportJsonBtn.addEventListener('click', exportQuestionsToJson);
  elements.viewLeaderboardBtn.addEventListener('click', openLeaderboard);

  // Close Modals
  document.querySelectorAll('[data-close-modal]').forEach((btn) => {
    btn.addEventListener('click', closeAllModals);
  });

  // Backdrop click close
  document.querySelectorAll('.modal-backdrop').forEach((backdrop) => {
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) closeAllModals();
    });
  });

  // Question Form Submission
  elements.questionForm.addEventListener('submit', handleQuestionFormSubmit);

  // Delete Confirm
  elements.confirmDeleteBtn.addEventListener('click', executeDelete);

  // Import Handling
  setupImportEvents();
}

function openModal(modal) {
  modal.classList.add('active');
}

function closeAllModals() {
  document.querySelectorAll('.modal-backdrop').forEach((m) => m.classList.remove('active'));
}

// ==================== CRUD OPERATIONS ====================

function openAddModal() {
  elements.modalTitle.textContent = 'Add New Question';
  elements.editDocId.value = '';
  elements.questionForm.reset();
  elements.formActive.checked = true;
  document.getElementById('radioOpt0').checked = true;
  openModal(elements.questionModal);
}

window.openEditModal = function(id) {
  const q = state.questions.find(item => item.id === id);
  if (!q) return;

  elements.modalTitle.textContent = 'Update Question';
  elements.editDocId.value = q.id;
  elements.formSubject.value = q.subject || 'DSA';
  elements.formDifficulty.value = q.difficulty || 'Easy';
  elements.formText.value = q.text || '';
  elements.formOpt0.value = q.options[0] || '';
  elements.formOpt1.value = q.options[1] || '';
  elements.formOpt2.value = q.options[2] || '';
  elements.formOpt3.value = q.options[3] || '';
  elements.formActive.checked = q.active;

  const rad = document.querySelector(`input[name="correctOption"][value="${q.answerIndex}"]`);
  if (rad) rad.checked = true;

  openModal(elements.questionModal);
};

async function handleQuestionFormSubmit(e) {
  e.preventDefault();

  const docId = elements.editDocId.value;
  const subject = elements.formSubject.value;
  const difficulty = elements.formDifficulty.value;
  const text = elements.formText.value.trim();
  const options = [
    elements.formOpt0.value.trim(),
    elements.formOpt1.value.trim(),
    elements.formOpt2.value.trim(),
    elements.formOpt3.value.trim()
  ];
  const selectedRadio = document.querySelector('input[name="correctOption"]:checked');
  const answerIndex = selectedRadio ? parseInt(selectedRadio.value, 10) : 0;
  const active = elements.formActive.checked;

  if (!text || options.some(opt => !opt)) {
    showToast('Please fill in the question and all 4 options', 'error');
    return;
  }

  const payload = {
    subject,
    difficulty,
    text,
    options,
    answerIndex,
    active
  };

  try {
    if (db) {
      if (docId) {
        await db.collection('questions').document(docId).update(payload);
        showToast('Question updated successfully!', 'success');
      } else {
        await db.collection('questions').add(payload);
        showToast('Question created successfully!', 'success');
      }
    } else {
      // Local fallback
      if (docId) {
        const idx = state.questions.findIndex(q => q.id === docId);
        if (idx !== -1) state.questions[idx] = { id: docId, ...payload };
      } else {
        state.questions.unshift({ id: 'local_' + Date.now(), ...payload });
      }
      applyFilters();
      updateDashboardStats();
      showToast('Saved in local session', 'success');
    }
    closeAllModals();
  } catch (error) {
    console.error('Error saving question:', error);
    showToast('Failed to save question: ' + error.message, 'error');
  }
}

// Quick Active Toggle
window.toggleQuestionActive = async function(id, isActive) {
  try {
    if (db) {
      await db.collection('questions').document(id).update({ active: isActive });
      showToast(`Question marked as ${isActive ? 'Active' : 'Inactive'}`, 'info');
    } else {
      const q = state.questions.find(item => item.id === id);
      if (q) q.active = isActive;
      updateDashboardStats();
    }
  } catch (err) {
    console.error('Failed to update status:', err);
    showToast('Failed to update status: ' + err.message, 'error');
  }
};

// Delete Operations
window.openDeleteModal = function(id) {
  const q = state.questions.find(item => item.id === id);
  if (!q) return;

  state.deleteTargetId = id;
  elements.deleteQuestionStatement.textContent = `"${q.text}"`;
  openModal(elements.deleteModal);
};

async function executeDelete() {
  if (!state.deleteTargetId) return;

  try {
    if (db) {
      await db.collection('questions').document(state.deleteTargetId).delete();
      showToast('Question deleted permanently', 'success');
    } else {
      state.questions = state.questions.filter(q => q.id !== state.deleteTargetId);
      applyFilters();
      updateDashboardStats();
      showToast('Question deleted locally', 'success');
    }
    closeAllModals();
  } catch (err) {
    console.error('Delete error:', err);
    showToast('Failed to delete: ' + err.message, 'error');
  } finally {
    state.deleteTargetId = null;
  }
}

// ==================== BULK IMPORT & EXPORT ====================

function setupImportEvents() {
  const dz = elements.dropZone;
  const fileInput = elements.jsonFileInput;

  dz.addEventListener('click', () => fileInput.click());

  dz.addEventListener('dragover', (e) => {
    e.preventDefault();
    dz.classList.add('dragover');
  });

  dz.addEventListener('dragleave', () => dz.classList.remove('dragover'));

  dz.addEventListener('drop', (e) => {
    e.preventDefault();
    dz.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) {
      handleFileSelected(e.dataTransfer.files[0]);
    }
  });

  fileInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) {
      handleFileSelected(e.target.files[0]);
    }
  });

  // Load sample bundled file
  elements.loadSampleJsonBtn.addEventListener('click', async () => {
    try {
      const res = await fetch('sample-questions.json');
      if (!res.ok) throw new Error('File not found');
      const data = await res.json();
      processParsedQuestions(data);
    } catch (err) {
      showToast('Could not load sample file: ' + err.message, 'error');
    }
  });

  elements.startImportBtn.addEventListener('click', executeBatchImport);
}

function handleFileSelected(file) {
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const data = JSON.parse(e.target.result);
      processParsedQuestions(data);
    } catch (err) {
      showToast('Invalid JSON file format: ' + err.message, 'error');
    }
  };
  reader.readAsText(file);
}

function processParsedQuestions(data) {
  if (!Array.isArray(data)) {
    showToast('Expected a JSON array of questions', 'error');
    return;
  }

  // Filter valid questions
  const valid = data.filter(q => q && q.text && Array.isArray(q.options) && q.options.length === 4);
  state.parsedImportQuestions = valid;

  elements.previewCountBadge.textContent = `${valid.length} valid questions`;
  elements.previewSample.innerHTML = valid.slice(0, 3).map(q => `
    <div style="margin-bottom: 6px; border-bottom: 1px dashed var(--border-subtle); padding-bottom: 4px;">
      <strong>[${escapeHtml(q.subject || 'DSA')}]</strong> ${escapeHtml(q.text)}
    </div>
  `).join('') + (valid.length > 3 ? `<div class="text-muted">+ ${valid.length - 3} more questions...</div>` : '');

  elements.importPreview.style.display = 'block';
  elements.startImportBtn.disabled = valid.length === 0;
}

// Normalize text for duplicate checking (matching QuestionManagementActivity.java)
function normalizeQuestionText(text) {
  if (!text) return '';
  return text.toLowerCase().trim().replace(/\s+/g, ' ').replace(/[^a-zA-Z0-9\s]/g, '');
}

async function executeBatchImport() {
  const questionsToImport = state.parsedImportQuestions;
  if (!questionsToImport.length) return;

  elements.startImportBtn.disabled = true;
  elements.importProgressWrapper.style.display = 'block';

  // Gather existing question normalized texts
  const existingSet = new Set(state.questions.map(q => normalizeQuestionText(q.text)));

  let imported = 0;
  let skipped = 0;
  const total = questionsToImport.length;

  if (db) {
    const BATCH_LIMIT = 100;
    let batch = db.batch();
    let batchCount = 0;

    for (let i = 0; i < total; i++) {
      const q = questionsToImport[i];
      const norm = normalizeQuestionText(q.text);

      if (existingSet.has(norm)) {
        skipped++;
      } else {
        existingSet.add(norm);
        const ref = db.collection('questions').doc();
        batch.set(ref, {
          subject: q.subject || 'DSA',
          difficulty: q.difficulty || 'Easy',
          text: q.text.trim(),
          options: q.options,
          answerIndex: typeof q.answerIndex === 'number' ? q.answerIndex : 0,
          active: q.active !== false
        });
        batchCount++;
        imported++;

        if (batchCount >= BATCH_LIMIT) {
          await batch.commit();
          batch = db.batch();
          batchCount = 0;
        }
      }

      // Progress Update
      const pct = Math.round(((i + 1) / total) * 100);
      elements.importProgressBar.style.width = `${pct}%`;
      elements.importProgressPercent.textContent = `${pct}%`;
      elements.importProgressStatus.textContent = `Processing ${i + 1} of ${total}...`;
    }

    if (batchCount > 0) {
      await batch.commit();
    }
  } else {
    // Local fallback
    questionsToImport.forEach(q => {
      const norm = normalizeQuestionText(q.text);
      if (existingSet.has(norm)) {
        skipped++;
      } else {
        existingSet.add(norm);
        state.questions.push({ id: 'imp_' + Math.random(), ...q });
        imported++;
      }
    });
    applyFilters();
    updateDashboardStats();
  }

  elements.importProgressStatus.textContent = 'Import Complete!';
  showToast(`Imported ${imported} questions! (${skipped} duplicates skipped)`, 'success');
  
  setTimeout(() => {
    closeAllModals();
    elements.importProgressWrapper.style.display = 'none';
  }, 1200);
}

// Export Questions to JSON File
function exportQuestionsToJson() {
  if (!state.questions.length) {
    showToast('No questions to export', 'error');
    return;
  }

  const exportData = state.questions.map(q => ({
    subject: q.subject,
    difficulty: q.difficulty,
    text: q.text,
    options: q.options,
    answerIndex: q.answerIndex,
    active: q.active
  }));

  const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `cs_quiz_questions_backup_${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
  showToast(`Exported ${exportData.length} questions to JSON`, 'success');
}

// ==================== LEADERBOARD ====================

function openLeaderboard() {
  const tbody = elements.leaderboardTableBody;
  tbody.innerHTML = '';

  const sorted = [...state.students].sort((a, b) => (b.score || 0) - (a.score || 0));

  if (!sorted.length) {
    elements.leaderboardEmpty.style.display = 'block';
  } else {
    elements.leaderboardEmpty.style.display = 'none';
    sorted.forEach((student, index) => {
      const tr = document.createElement('tr');
      let medal = `#${index + 1}`;
      if (index === 0) medal = '🥇 1st';
      if (index === 1) medal = '🥈 2nd';
      if (index === 2) medal = '🥉 3rd';

      tr.innerHTML = `
        <td style="font-weight: 700; color: ${index < 3 ? 'var(--warning)' : 'var(--text-muted)'}">${medal}</td>
        <td style="font-weight: 500;">${escapeHtml(student.email)}</td>
        <td><span class="badge ${student.role === 'admin' ? 'badge-subject-networks' : 'badge-subject-dsa'}">${escapeHtml(student.role)}</span></td>
        <td style="text-align: right; font-weight: 700; color: var(--success); font-size: 1.05rem;">${student.score || 0} pts</td>
      `;
      tbody.appendChild(tr);
    });
  }

  openModal(elements.leaderboardModal);
}

// Fallback initial dummy data if Firestore is unreachable
function loadFallbackData() {
  state.questions = [
    {
      id: 'demo_1',
      subject: 'OS',
      difficulty: 'Easy',
      text: 'Which scheduling algorithm is non-preemptive?',
      options: ['Round Robin', 'First Come First Serve (FCFS)', 'Shortest Remaining Time First', 'Priority (Preemptive)'],
      answerIndex: 1,
      active: true
    },
    {
      id: 'demo_2',
      subject: 'DBMS',
      difficulty: 'Medium',
      text: 'Which normal form eliminates transitive dependency?',
      options: ['1NF', '2NF', '3NF', 'BCNF'],
      answerIndex: 2,
      active: true
    },
    {
      id: 'demo_3',
      subject: 'DSA',
      difficulty: 'Medium',
      text: 'What is the average time complexity of searching in a Hash Table?',
      options: ['O(1)', 'O(n)', 'O(log n)', 'O(n log n)'],
      answerIndex: 0,
      active: true
    },
    {
      id: 'demo_4',
      subject: 'Networks',
      difficulty: 'Easy',
      text: 'Which protocol is responsible for mapping IP addresses to MAC addresses?',
      options: ['DNS', 'DHCP', 'ARP', 'ICMP'],
      answerIndex: 2,
      active: true
    }
  ];
  applyFilters();
  updateDashboardStats();
}
