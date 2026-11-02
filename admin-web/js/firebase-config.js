// Firebase Configuration matching project22 (quiz-ec6eb)
const firebaseConfig = {
  apiKey: "AIzaSyCgaYR8WPe2Bk5EUD0lZfpDRVrnOOzljRw",
  authDomain: "quiz-ec6eb.firebaseapp.com",
  projectId: "quiz-ec6eb",
  storageBucket: "quiz-ec6eb.firebasestorage.app",
  messagingSenderId: "617212650561",
  appId: "1:617212650561:web:quiz-admin-portal"
};

// Initialize Firebase
let firebaseApp = null;
let auth = null;
let db = null;

try {
  if (typeof firebase !== 'undefined') {
    firebaseApp = firebase.initializeApp(firebaseConfig);
    auth = firebase.auth();
    db = firebase.firestore();
    console.log("🔥 Firebase initialized successfully for Quiz App Admin Portal");
  } else {
    console.warn("⚠️ Firebase SDK not loaded via script tags.");
  }
} catch (error) {
  console.error("Firebase init error:", error);
}
