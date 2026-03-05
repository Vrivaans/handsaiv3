import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, TokenStatus } from '../auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-token-management',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './token-management.html',
  styleUrls: ['./token-management.scss']
})
export class TokenManagementComponent implements OnInit {

  tokenStatus: TokenStatus | null = null;

  loading = true;
  error = '';

  // For regeneration
  rawToken = '';
  warning = '';
  showConfirm = false;
  copied = false;

  constructor(private authService: AuthService, private router: Router) { }

  ngOnInit(): void {
    this.loadStatus();
  }

  loadStatus() {
    this.loading = true;
    this.error = '';
    this.authService.getTokenStatus().subscribe({
      next: (status) => {
        this.tokenStatus = status;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          // Session expired or not logged in, redirect to login
          this.router.navigate(['/auth/login']);
        } else {
          this.error = 'Failed to load token status.';
        }
      }
    });
  }

  confirmRegenerate() {
    this.showConfirm = true;
    this.rawToken = '';
  }

  cancelRegenerate() {
    this.showConfirm = false;
  }

  doRegenerate() {
    this.loading = true;
    this.authService.regenerateToken().subscribe({
      next: (res) => {
        this.rawToken = res.rawToken;
        this.warning = res.warning;
        this.showConfirm = false;
        this.loadStatus(); // refresh status
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Failed to regenerate token.';
      }
    });
  }

  copyToken() {
    if (this.rawToken) {
      navigator.clipboard.writeText(this.rawToken).then(() => {
        this.copied = true;
        setTimeout(() => this.copied = false, 2000);
      });
    }
  }

  logout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/home']);
    });
  }
}
