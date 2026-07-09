/**
 * Évalue une expression de dépendance d'affichage `visibleIf` du catalogue.
 * Formes supportées (miroir du backend) : "var == valeur", "var != valeur", "var in [a,b]".
 */
export function isVisible(expr: string | null | undefined, resolve: (name: string) => unknown): boolean {
  if (!expr) return true
  const e = expr.trim()

  const inMatch = e.match(/^(\w+)\s+in\s+\[(.*)\]$/)
  if (inMatch) {
    const [, name, list] = inMatch
    const opts = list.split(',').map((s) => s.trim().replace(/^['"]|['"]$/g, ''))
    return opts.includes(String(resolve(name) ?? ''))
  }

  const eq = e.match(/^(\w+)\s*(==|!=)\s*(.+)$/)
  if (eq) {
    const [, name, op, rawVal] = eq
    const expected = rawVal.trim().replace(/^['"]|['"]$/g, '')
    const actual = String(resolve(name) ?? '')
    return op === '==' ? actual === expected : actual !== expected
  }

  return true
}
