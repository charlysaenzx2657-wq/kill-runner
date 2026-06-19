# KILL Runner

App Android profesional para liberar RAM matando procesos en segundo plano, dejando protegida tu app favorita.

## ¿Qué hace?

1. **Elige tu app** — selecciona la app que quieres proteger (Free Fire, cualquier juego, etc.)
2. **Toca "Lanzar y Limpiar"** — la app protegida se abre y se eliminan todos los procesos en background
3. **Ve los resultados** — cuántos procesos eliminó y cuánta RAM liberó

## Permiso requerido

```
KILL_BACKGROUND_PROCESSES
```
Este permiso es 100% compatible con Play Store. No requiere root ni Shizuku.

---

## Compilar con GitHub Actions

### 1. Sube el proyecto a GitHub

```bash
git init
git add .
git commit -m "Initial commit - KILL Runner"
git remote add origin https://github.com/TU_USUARIO/kill-runner.git
git push -u origin main
```

### 2. El workflow se ejecuta automáticamente

Cada push a `main` compila el APK. Ve a:
`Repositorio → Actions → Build KILL Runner APK → Artifacts`

Descarga:
- `KillRunner-Debug.apk` — para pruebas
- `KillRunner-Release-unsigned.apk` — para firmar y subir a Play Store

---

## Para Play Store (firmar el APK)

### Generar keystore (una sola vez)

```bash
keytool -genkey -v -keystore kill-runner.keystore \
  -alias kill-runner-key \
  -keyalg RSA -keysize 2048 -validity 10000
```

### Configurar secrets en GitHub

Ve a `Settings → Secrets → Actions` y agrega:

| Secret | Valor |
|--------|-------|
| `KEYSTORE_BASE64` | `base64 kill-runner.keystore` |
| `KEY_ALIAS` | `kill-runner-key` |
| `KEYSTORE_PASSWORD` | tu contraseña |
| `KEY_PASSWORD` | tu contraseña de key |

### Descomenta el job `sign` en `.github/workflows/build.yml`

---

## Estructura del proyecto

```
KillRunner/
├── app/src/main/
│   ├── java/com/killrunner/
│   │   ├── MainActivity.kt        ← Pantalla principal
│   │   ├── SelectAppActivity.kt   ← Selector de apps
│   │   ├── AppAdapter.kt          ← Lista de apps
│   │   └── AppInfo.kt             ← Modelo de datos
│   ├── res/layout/
│   │   ├── activity_main.xml
│   │   ├── activity_select_app.xml
│   │   └── item_app.xml
│   └── AndroidManifest.xml
└── .github/workflows/
    └── build.yml                  ← GitHub Actions
```
