import http, { unwrap } from './http'

export function getScriptSchema() {
  return unwrap<string>(http.get('/schema/script'))
}

export function getScriptSchemaDesign() {
  return unwrap<string>(http.get('/schema/script/design'))
}

export const schemaApi = {
  getScriptSchema,
  getScriptSchemaDesign
}
