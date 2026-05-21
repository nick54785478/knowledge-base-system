import { Component, OnInit } from '@angular/core';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Router } from '@angular/router';
import { SharedModule } from 'primeng/api';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dialog-form',
  standalone: true,
  imports: [CommonModule],
  providers: [DialogService, DynamicDialogRef],
  templateUrl: './dialog-form.component.html',
  styleUrl: './dialog-form.component.scss',
})
export class DialogFormComponent implements OnInit {
  constructor(public ref: DynamicDialogRef) {}
  ngOnInit(): void {}
}
