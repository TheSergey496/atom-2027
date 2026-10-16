import { Directive, inject, OnDestroy, OnInit } from '@angular/core';
import { BlockUI } from '@openng/optimus-ui/blockui';

@Directive({
  selector: 'p-blockUI[proLegacyBlockedForLive]',
})
export class BlockedForLiveDirective implements OnInit, OnDestroy {
  private readonly blockUI = inject(BlockUI);

  ngOnInit(): void {
    this.blockUI.blocked = true;
  }

  ngOnDestroy(): void {
    this.blockUI.destroyModal();
  }
}
