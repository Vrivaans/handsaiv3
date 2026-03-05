import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../environments/environment';

export interface AuthStatus {
    setupComplete: boolean;
    locked: boolean;
    lockedUntil?: string;
    failedAttempts: number;
    maxAttempts: number;
}

export interface SetupRequest {
    username?: string;
    password?: string;
}

export interface LoginRequest {
    username?: string;
    password?: string;
}

export interface TokenStatus {
    exists: boolean;
    maskedToken?: string;
    createdAt?: string;
    lastUsedAt?: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = `${environment.apiUrl}/admin`;

    // Track if we have a valid admin session
    private _isLoggedIn = new BehaviorSubject<boolean>(false);
    public isLoggedIn$ = this._isLoggedIn.asObservable();

    // Track if the initial setup is complete
    private _isSetupComplete = new BehaviorSubject<boolean>(true); // assume true until checked
    public isSetupComplete$ = this._isSetupComplete.asObservable();

    constructor(private http: HttpClient) {
        this.checkSession();
    }

    getAuthStatus(): Observable<AuthStatus> {
        return this.http.get<AuthStatus>(`${this.apiUrl}/auth/status`).pipe(
            tap(status => this._isSetupComplete.next(status.setupComplete))
        );
    }

    setup(credentials: SetupRequest): Observable<{ rawToken: string, warning: string }> {
        return this.http.post<{ rawToken: string, warning: string }>(`${this.apiUrl}/auth/setup`, credentials).pipe(
            tap(() => {
                this._isSetupComplete.next(true);
            })
        );
    }

    login(credentials: LoginRequest): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/auth/login`, credentials).pipe(
            tap(() => this._isLoggedIn.next(true))
        );
    }

    logout(): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/auth/logout`, {}).pipe(
            tap(() => this._isLoggedIn.next(false)),
            catchError(() => {
                this._isLoggedIn.next(false);
                return of(null);
            })
        );
    }

    checkSession(): void {
        this.http.get<{ valid: boolean }>(`${this.apiUrl}/auth/session`).subscribe({
            next: (res) => this._isLoggedIn.next(res.valid),
            error: () => this._isLoggedIn.next(false)
        });
    }

    getTokenStatus(): Observable<TokenStatus> {
        return this.http.get<TokenStatus>(`${this.apiUrl}/token/status`);
    }

    regenerateToken(): Observable<{ rawToken: string, warning: string }> {
        return this.http.post<{ rawToken: string, warning: string }>(`${this.apiUrl}/token/regenerate`, {});
    }
}
