import { Component } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'pro-legacy-loader',
  standalone: true,
  templateUrl: './loader.component.html',
  styleUrl: './loader.component.css',
  imports: [NgOptimizedImage],
})
// eslint-disable-next-line @typescript-eslint/no-extraneous-class
export class LoaderComponent {}
