# MotoTaller PRO — Sistema Integral de Taller de Motos

Arquitectura conectada en tiempo real:
* **Móvil Android (Mecánicos & Patio):** Aplicación nativa en Jetpack Compose, modo mecánico con cronómetro de mano de obra, checklist express y firma digital.
* **Web Recepción (Mostrador & Administración):** Aplicación React optimizada para pantallas grandes, lista para desplegar en **Vercel**.
* **Base de Datos & Auth:** Firebase Firestore en tiempo real con autenticación Google Sign-In mediante Android Credential Manager, con distinción de roles (Administrador vs. Mecánico).

---

## Despliegue en Vercel (Web Recepción)

El código fuente del panel de recepción web se encuentra en la carpeta `/web` con configuración `vercel.json` lista para producción.

### Pasos para desplegar en Vercel:
1. Conecta el repositorio en [Vercel](https://vercel.com).
2. Selecciona el directorio raíz o `/web` como Root Directory.
3. El comando de build configurado es `npm run build` y el directorio de salida es `dist`.
4. ¡Listo! El mostrador de recepción reflejará en vivo los cambios que los mecánicos realicen desde la app móvil.
