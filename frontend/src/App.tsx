import { useState } from 'react'
import { setSession } from './api'
import AuthForm from './components/AuthForm'
import LineupPage from './components/LineupPage'
import LeaguesPage from './components/LeaguesPage'
import AdminPage from './components/AdminPage'

type Tab = 'lineup' | 'leagues' | 'admin'

export default function App() {
  const [user, setUser] = useState<string | null>(
    localStorage.getItem('token') ? localStorage.getItem('username') : null,
  )
  const [tab, setTab] = useState<Tab>('lineup')
  const [role, setRole] = useState(localStorage.getItem('role') ?? 'USER')

  if (!user) return <AuthForm onAuth={(name) => { setRole(localStorage.getItem('role') ?? 'USER'); setUser(name) }} />

  const logout = () => {
    setSession(null)
    setUser(null)
    setRole('USER')
  }

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">Fantasy<span>Arena</span></div>
        <nav className="main-nav">
          <button className={tab === 'lineup' ? 'active' : ''} onClick={() => setTab('lineup')}><span>⚡</span> Escalação</button>
          <button className={tab === 'leagues' ? 'active' : ''} onClick={() => setTab('leagues')}><span>🏆</span> Minhas ligas</button>
          {role === 'ADMIN' && <button className={tab === 'admin' ? 'active' : ''} onClick={() => setTab('admin')}><span>⚙</span> Administração</button>}
        </nav>
        <div className="user">
          {user} <button className="ghost" onClick={logout}>Sair</button>
        </div>
      </header>
      <main>{tab === 'lineup' ? <LineupPage /> : tab === 'leagues' ? <LeaguesPage /> : <AdminPage />}</main>
    </div>
  )
}
