import { useEffect, useMemo, useState } from 'react'
import { api, Athlete, EventItem, Lineup, LineupSummary, Sport } from '../api'

const LABEL: Record<string,string> = { DRIVER:'Pilotos', TEAM:'Equipes', GOALKEEPER:'Goleiros', DEFENDER:'Defensores', MIDFIELDER:'Meias', ATTACKER:'Atacantes', FIGHTER:'Lutadores' }
const fmt = (iso:string) => new Date(iso).toLocaleString('pt-BR', { dateStyle:'medium', timeStyle:'short' })

function PickSlot({athlete,label,onFocus,onRemove,compact=false}:{athlete?:Athlete;label:string;onFocus:()=>void;onRemove:()=>void;compact?:boolean}){
  return <div className={athlete?'visual-slot filled':'visual-slot'}>
    <button type="button" className={compact?'slot-ball compact':'slot-ball'} onClick={onFocus} title={athlete?`Trocar ${athlete.name}`:`Escolher ${label}`}>
      {athlete?.imageUrl?<img src={athlete.imageUrl} alt={athlete.name}/>:athlete?<span className="slot-initials">{athlete.name.slice(0,2).toUpperCase()}</span>:<span className="slot-plus">+</span>}
    </button>
    <span className="slot-name">{athlete?.name??label}</span>
    {athlete&&<button type="button" className="slot-remove" onClick={onRemove} aria-label={`Remover ${athlete.name}`}>×</button>}
  </div>
}

export default function LineupPage() {
  const [sports,setSports]=useState<Sport[]>([]), [sport,setSport]=useState<Sport|null>(null)
  const [events,setEvents]=useState<EventItem[]>([]), [event,setEvent]=useState<EventItem|null>(null)
  const [athletes,setAthletes]=useState<Athlete[]>([]), [selected,setSelected]=useState<Set<number>>(new Set())
  const [saved,setSaved]=useState<Lineup|null>(null), [history,setHistory]=useState<LineupSummary[]>([])
  const [loading,setLoading]=useState(true), [message,setMessage]=useState('')

  useEffect(()=>{ Promise.all([api.sports(),api.history()]).then(([s,h])=>{setSports(s);setHistory(h);setSport(s[0]??null)}).catch(e=>setMessage(e.message)).finally(()=>setLoading(false)) },[])
  useEffect(()=>{ if(!sport)return; setMessage(''); setSelected(new Set()); setSaved(null); Promise.all([api.events(sport.code),api.athletes(sport.code)]).then(([ev,a])=>{setEvents(ev);setAthletes(a);setEvent(ev.filter(x=>!x.locked&&x.status==='SCHEDULED').sort((a,b)=>a.startsAt.localeCompare(b.startsAt))[0]??null)}).catch(e=>setMessage(e.message)) },[sport])
  useEffect(()=>{ if(!event){setSelected(new Set());return} api.lineup(event.id).then(l=>{setSaved(l);setSelected(new Set(l?.picks.map(p=>p.id)??[]))}).catch(e=>setMessage(e.message)) },[event])
  const byId=useMemo(()=>new Map(athletes.map(a=>[a.id,a])),[athletes])
  const spent=[...selected].reduce((n,id)=>n+(byId.get(id)?.price??0),0)
  const count=(kind:string)=>[...selected].filter(id=>byId.get(id)?.kind===kind).length
  const picks=(kind:string)=>[...selected].map(id=>byId.get(id)).filter((a):a is Athlete=>a?.kind===kind)
  const valid=!!sport&&Object.entries(sport.slots).every(([k,n])=>count(k)===n)&&spent<=sport.budget
  const toggle=(a:Athlete)=>{ if(!sport)return; const n=new Set(selected); if(n.has(a.id))n.delete(a.id);else if(count(a.kind)<(sport.slots[a.kind]??0))n.add(a.id);setSelected(n) }
  const remove=(id:number)=>{const n=new Set(selected);n.delete(id);setSelected(n)}
  const focus=(kind:string)=>document.getElementById(`roster-${kind}`)?.scrollIntoView({behavior:'smooth',block:'start'})
  const save=async()=>{if(!event)return;try{setSaved(await api.saveLineup(event.id,[...selected]));setMessage('Escalação salva com sucesso.')}catch(e){setMessage((e as Error).message)}}
  if(loading)return <div className="empty">Carregando sua arena…</div>
  return <>
    <div className="page-heading"><div><span className="eyebrow">MERCADO</span><h1>Monte sua escalação</h1><p>Escolha a modalidade, o evento e os melhores atletas dentro do orçamento.</p></div></div>
    <section className="sport-picker">{sports.map(s=><button key={s.code} className={sport?.code===s.code?'sport-option active':'sport-option'} onClick={()=>setSport(s)}><span className="sport-icon">{s.code==='F1'?'🏎':s.code==='UFC'?'🥊':'⚽'}</span><span><strong>{s.name}</strong><small>Orçamento {s.budget.toFixed(0)}</small></span></button>)}</section>
    {sport&&<section className="card market-panel"><div className="field"><label>Evento aberto</label><select value={event?.id??''} onChange={e=>setEvent(events.find(x=>x.id===Number(e.target.value))??null)}><option value="">Selecione um evento</option>{events.filter(e=>!e.locked&&e.status==='SCHEDULED').map(e=><option key={e.id} value={e.id}>{e.name} — {fmt(e.startsAt)}</option>)}</select></div>{event&&<><div className="market-title"><div><span className="status-dot"/> Mercado aberto</div><strong>{event.name}</strong><span>Fecha em {fmt(event.startsAt)}</span></div><div className="slots">{Object.entries(sport.slots).map(([k,n])=><span className={count(k)===n?'chip done':'chip'} key={k}>{LABEL[k]??k}: {count(k)}/{n}</span>)}<span className={spent>sport.budget?'chip over':'chip'}>💰 {spent.toFixed(1)} / {sport.budget.toFixed(1)}</span></div></>}{!event&&<div className="empty compact">Não há eventos abertos nesta modalidade. No UFC, um administrador precisa cadastrar o próximo card.</div>}</section>}
    {event&&sport&&<div className="lineup-layout"><aside className={`lineup-builder builder-${sport.code.toLowerCase()}`}><div className="builder-head"><div><span className="eyebrow">SUA ESCALAÇÃO</span><h2>{sport.code==='FOOTBALL'?'Formação 1–4–4–2':sport.code==='F1'?'Seu grid':'Seu time de luta'}</h2></div><strong>{selected.size}/{Object.values(sport.slots).reduce((a,b)=>a+b,0)}</strong></div>
      {sport.code==='FOOTBALL'?<div className="football-pitch"><div className="pitch-line pitch-attack">{Array.from({length:2},(_,i)=><PickSlot compact key={`a${i}`} athlete={picks('ATTACKER')[i]} label="ATA" onFocus={()=>focus('ATTACKER')} onRemove={()=>picks('ATTACKER')[i]&&remove(picks('ATTACKER')[i].id)}/>)}</div><div className="pitch-line">{Array.from({length:4},(_,i)=><PickSlot compact key={`m${i}`} athlete={picks('MIDFIELDER')[i]} label="MEI" onFocus={()=>focus('MIDFIELDER')} onRemove={()=>picks('MIDFIELDER')[i]&&remove(picks('MIDFIELDER')[i].id)}/>)}</div><div className="pitch-line">{Array.from({length:4},(_,i)=><PickSlot compact key={`d${i}`} athlete={picks('DEFENDER')[i]} label="DEF" onFocus={()=>focus('DEFENDER')} onRemove={()=>picks('DEFENDER')[i]&&remove(picks('DEFENDER')[i].id)}/>)}</div><div className="pitch-line pitch-goal">{<PickSlot compact athlete={picks('GOALKEEPER')[0]} label="GOL" onFocus={()=>focus('GOALKEEPER')} onRemove={()=>picks('GOALKEEPER')[0]&&remove(picks('GOALKEEPER')[0].id)}/>}</div></div>:sport.code==='F1'?<div className="f1-board"><span className="board-label">PILOTOS</span><div className="driver-slots">{Array.from({length:2},(_,i)=><PickSlot key={i} athlete={picks('DRIVER')[i]} label={`Piloto ${i+1}`} onFocus={()=>focus('DRIVER')} onRemove={()=>picks('DRIVER')[i]&&remove(picks('DRIVER')[i].id)}/>)}</div><span className="board-label">EQUIPE</span><PickSlot athlete={picks('TEAM')[0]} label="Escolher equipe" onFocus={()=>focus('TEAM')} onRemove={()=>picks('TEAM')[0]&&remove(picks('TEAM')[0].id)}/></div>:<div className="ufc-board">{Array.from({length:5},(_,i)=><PickSlot key={i} athlete={picks('FIGHTER')[i]} label={`Lutador ${i+1}`} onFocus={()=>focus('FIGHTER')} onRemove={()=>picks('FIGHTER')[i]&&remove(picks('FIGHTER')[i].id)}/>)}</div>}
      <div className="builder-budget"><span>Orçamento usado</span><strong className={spent>sport.budget?'over':''}>{spent.toFixed(1)} <small>/ {sport.budget.toFixed(1)}</small></strong><div><i style={{width:`${Math.min(100,spent/sport.budget*100)}%`}}/></div></div><button className="primary full" disabled={!valid} onClick={save}>{saved?'Atualizar escalação':'Salvar escalação'}</button></aside>
      <div className="athlete-market">{Object.keys(sport.slots).map(kind=><section id={`roster-${kind}`} className="roster" key={kind}><div className="section-title"><h2>{LABEL[kind]??kind}</h2><span>{count(kind)}/{sport.slots[kind]} escolhido(s)</span></div><div className="grid">{athletes.filter(a=>a.kind===kind).map(a=><button key={a.id} className={selected.has(a.id)?'athlete selected':'athlete'} onClick={()=>toggle(a)}>{a.imageUrl?<img src={a.imageUrl} alt=""/>:<span className="avatar">{a.name.slice(0,2).toUpperCase()}</span>}<div className="name">{a.name}</div><div className="muted small">{a.team||LABEL[a.kind]}</div><div className="price">{a.price.toFixed(1)} moedas</div><span className="pick-mark">✓</span></button>)}</div></section>)}</div></div>}
    {message&&<div className="notice">{message}</div>}
    <section className="card"><div className="section-title"><h2>Histórico de rodadas</h2><span>{history.length} escalações</span></div>{!history.length?<div className="empty compact">Suas escalações aparecerão aqui.</div>:<div className="table-wrap"><table><thead><tr><th>Evento</th><th>Status</th><th className="num">Pontos</th></tr></thead><tbody>{history.map(h=><tr key={h.eventId}><td><strong>{h.eventName}</strong></td><td><span className="badge">{h.scored?'Pontuado':'Aguardando'}</span></td><td className="num">{h.scored?h.totalPoints.toFixed(1):'—'}</td></tr>)}</tbody></table></div>}</section>
  </>
}
