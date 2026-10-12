import type { E2EConfig } from 'e2e';
import { mobile } from '@e2e-dev/mobile';

// Deterministic, model-free suite: every step uses `screen` + `expect` against
// the TalkBack-facing accessibility tree. No agent steps, so no model and no
// screenshots leave the machine.
export default {
  targets: [
    {
      name: 'android',
      engine: mobile({ platform: 'android' }),
      app: { bundleId: 'com.aistudio.quranblind.a11y' },
    },
  ],
  workers: 1,
} satisfies E2EConfig;
