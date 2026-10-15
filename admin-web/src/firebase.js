import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';

/**
 * Firebase project: stickersbank-90dc9  (same project the Android app uses).
 *
 * If Firestore calls fail with "API key not valid" / "requests-from-referer blocked",
 * register a WEB app in the Firebase console and paste its config here:
 *   Firebase console → Project settings → Your apps → Add app → Web (</>)
 * Then copy apiKey + appId from the snippet it shows you.
 */
const firebaseConfig = {
  apiKey: "AIzaSyCLRNsF3f9cteFfIqrqkSC5WjrCk29G_Vc",
  authDomain: "stickersbank-4c5bb.firebaseapp.com",
  projectId: "stickersbank-4c5bb",
  storageBucket: "stickersbank-4c5bb.firebasestorage.app",
  messagingSenderId: "924829000161",
  appId: "1:924829000161:web:6e000ee0231f1b69e832a0"
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
