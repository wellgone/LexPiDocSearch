/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_ES_URL: string
  readonly VITE_ES_USERNAME: string
  readonly VITE_ES_PASSWORD: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
} 