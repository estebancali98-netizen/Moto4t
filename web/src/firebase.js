import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';
import { getAuth } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyBj6ET4BxiSbKPDBiAgtk1Tw4QdFU_1W7s",
  projectId: "gen-lang-client-0068345644",
  storageBucket: "gen-lang-client-0068345644.firebasestorage.app",
  appId: "1:874094866591:android:443fdc54e40cab9f9332aa"
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app, "ai-studio-android-mototall-9d64eac3-6d93-4c42-ad56-bf5ef7c6b7eb");
