import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { BrigadeService } from '../../../gen/atom2027-controllers';
import { lastValueFrom } from 'rxjs';

@Component({
  imports: [],
  selector: 'pro-legacy-brigade-events',
  styleUrl: './brigade-events.component.css',
  templateUrl: './brigade-events.component.html',
})
export class BrigadeEventsComponent implements OnInit, OnDestroy {
  private readonly brigadeService = inject(BrigadeService);

  async ngOnInit() {
    await lastValueFrom(this.brigadeService.getBrigadeEvents());
  }

  ngOnDestroy(): void {}
}
