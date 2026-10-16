export interface User {id: string; displayName: string; email: string}
export interface Session {authenticated: boolean; user?: User}
export interface Trip {id: string; name: string; destination: string; startDate: string; endDate: string; currency: string; inviteCode: string}
export interface Member {id: string; displayName: string; email: string; role: string}
export interface Activity {id: string; date: string; startTime?: string; title: string; type: string; status: string; place?: string; notes?: string; reservationReference?: string; bookingUrl?: string; voteScore: number; currentUserVote: number}
export interface Expense {id: string; description: string; paidByMemberId: string; category: string; splitMode: string; occurredOn: string; totalCents: number; shares: {memberId: string; totalCents: number}[]}
export interface Balance {memberId: string; displayName: string; amountCents: number}
export interface Transfer {fromMemberId: string; fromName: string; toMemberId: string; toName: string; amountCents: number}
export interface Settlement {strategyUsed: string; fellBackFromOptimal: boolean; transfers: Transfer[]}
export interface TripData {trip: Trip; members: Member[]; activities: Activity[]; expenses: Expense[]; balances: Balance[]; settlement: Settlement}
