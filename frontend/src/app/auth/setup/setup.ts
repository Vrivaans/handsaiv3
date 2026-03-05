import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth.service';

@Component({
  selector: 'app-setup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './setup.html',
  styleUrls: ['./setup.scss']
})
export class SetupComponent {
  username = '';
  password = '';
  confirmPassword = '';

  error = '';
  loading = false;

  // After success
  rawToken = '';
  warning = '';
  copied = false;

  constructor(private authService: AuthService, private router: Router) { }

  onSubmit() {
    this.error = '';

    if (!this.username.trim()) {
      this.error = 'Username is required';
      return;
    }

    if (this.password.length < 8) {
      this.error = 'Password must be at least 8 characters';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.error = 'Passwords do not match';
      return;
    }

    this.loading = true;
    this.authService.setup({ username: this.username, password: this.password }).subscribe({
      next: (res) => {
        this.rawToken = res.rawToken;
        this.warning = res.warning;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || err.error?.error || 'Setup failed';
        this.loading = false;
      }
    });
  }

  copyToken() {
    navigator.clipboard.writeText(this.rawToken).then(() => {
      this.copied = true;
      setTimeout(() => this.copied = false, 2000);
    });
  }

  finishSetup() {
    this.router.navigate(['/home']);
  }
}
