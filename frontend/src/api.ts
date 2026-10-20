export interface Sport { code: string; name: string; budget: number; slots: Record<string, number> }
export interface EventItem { id: number; name: string; startsAt: string; status: string; locked: boolean }
export interface Athlete { id: number; name: string; team: string | null; kind: string; price: number; imageUrl: string | null }
export interface Pick { id: number; name: string; team: string | null; kind: string; points: number | null }
export interface Lineup { eventId: number; eventName: string; scored: boolean; totalPoints: number; picks: Pick[] }
export interface LineupSummary { eventId: number; eventName: string; startsAt: string; scored: boolean; totalPoints: number }
export interface League { id: number; name: string; sport: string; inviteCode: string; members: number }
export interface RankingRow { username: string; points: number; rounds: number }
export interface Auth { token: string; username: string; role: 'USER' | 'ADMIN' }
export interface Me { id: number; username: string; role: 'USER' | 'ADMIN' }
export interface AdminDashboard { users: number; events: number; athletes: number; leagues: number; sportmonksConfigured: boolean }
export interface AdminUser { id: number; username: string; role: 'USER' | 'ADMIN' }
export interface AuditItem { action: string; entityType: string; entityId: string | null; details: string | null; createdAt: string }

let token: string | null = localStorage.getItem('token')

export function setSession(auth: Auth | null) {
  token = auth?.token ?? null
  if (auth) {
    localStorage.setItem('token', auth.token)
    localStorage.setItem('username', auth.username)
    localStorage.setItem('role', auth.role)
  } else {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch('/api' + path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
  if (res.status === 401 && token) {
    setSession(null)
    window.location.reload()
  }
  if (res.status === 204) return null as T
  if (!res.ok) {
    let message = 'Algo deu errado'
    try {
      message = (await res.json()).message || message
    } catch { /* body was not JSON */ }
    throw new Error(message)
  }
  return res.json()
}

const json = (body: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(body) })

export const api = {
  login: (username: string, password: string) => request<Auth>('/auth/login', json({ username, password })),
  register: (username: string, password: string) => request<Auth>('/auth/register', json({ username, password })),
  me: () => request<Me>('/auth/me'),
  sports: () => request<Sport[]>('/sports'),
  events: (sport: string) => request<EventItem[]>(`/sports/${sport}/events`),
  athletes: (sport: string) => request<Athlete[]>(`/sports/${sport}/athletes`),
  lineup: (eventId: number) => request<Lineup | null>(`/events/${eventId}/lineup`),
  saveLineup: (eventId: number, athleteIds: number[]) =>
    request<Lineup>(`/events/${eventId}/lineup`, { method: 'PUT', body: JSON.stringify({ athleteIds }) }),
  history: () => request<LineupSummary[]>('/lineups/mine'),
  leagues: () => request<League[]>('/leagues'),
  createLeague: (name: string, sport: string) => request<League>('/leagues', json({ name, sport })),
  joinLeague: (inviteCode: string) => request<League>('/leagues/join', json({ inviteCode })),
  ranking: (id: number) => request<RankingRow[]>(`/leagues/${id}/ranking`),
  adminDashboard: () => request<AdminDashboard>('/admin/dashboard'),
  adminUsers: () => request<AdminUser[]>('/admin/users'),
  setUserRole: (id: number, role: string) => request<AdminUser>(`/admin/users/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) }),
  adminFighters: () => request<Athlete[]>('/admin/ufc/fighters'),
  createFighter: (body: object) => request<Athlete>('/admin/ufc/fighters', json(body)),
  updateFighter: (id: number, body: object) => request<Athlete>(`/admin/ufc/fighters/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  adminUfcEvents: () => request<EventItem[]>('/admin/ufc/events'),
  createUfcEvent: (body: object) => request<EventItem>('/admin/ufc/events', json(body)),
  scoreUfcEvent: (id: number, points: Record<number, number>) => request<EventItem>(`/admin/ufc/events/${id}/score`, json({ points })),
  syncSports: () => request<{ message: string }>('/admin/sync', json({})),
  adminAudit: () => request<AuditItem[]>('/admin/audit'),
}
