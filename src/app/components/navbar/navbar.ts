import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive } from '@angular/router';
@Component({
  selector: 'app-navbar',
  imports: [
  MatButtonModule,
  MatIconModule,
  MatToolbarModule,
  RouterLink,
  RouterLinkActive
],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {}