import { definePreset } from '@openng/optimus-ui-themes';
import Material from '@openng/optimus-ui-themes/material';

export const DefaultTheme = definePreset(Material, {
  primitive: {
    blue: {
      50: '#E8F4FD',
      100: '#D6EAFB',
      200: '#B7D9F5',
      300: '#8FC3ED',
      400: '#559BDD',
      500: '#2575DC',
      600: '#2060AF',
      700: '#1B5195',
      800: '#17447C',
      900: '#123760',
    },
    yellow: {
      '50': '#FFF9EB',
      '100': '#FFF0CC',
      '200': '#F9E0A3',
      '300': '#F0CE79',
      '400': '#E3B953',
      '500': '#D1A137',
      '600': '#B98929',
      '700': '#966B24',
      '800': '#765321',
      '900': '#5B421F',
    },
    orange: {
      '50': '#FFF4E8',
      '100': '#FFE6CC',
      '200': '#F9D0A3',
      '300': '#F0B579',
      '400': '#E69650',
      '500': '#D77A32',
      '600': '#B96328',
      '700': '#965023',
      '800': '#764020',
      '900': '#5B331D',
    },
    green: {
      '50': '#EFF9F2',
      '100': '#D9F0E0',
      '200': '#B8E1C8',
      '300': '#8FCDA8',
      '400': '#65B786',
      '500': '#469D6B',
      '600': '#378657',
      '700': '#2E6D49',
      '800': '#28573D',
      '900': '#224632',
    },
    borderRadius: {
      xs: '4px',
      sm: '6px',
      md: '8px',
      lg: '10px',
      xl: '14px',
    },
  },

  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      200: '{blue.200}',
      300: '{blue.300}',
      400: '{blue.400}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}',
      800: '{blue.800}',
      900: '{blue.900}',
    },

    colorScheme: {
      light: {
        surface: {
          0: '#FFFFFF',
          50: '#FDFDFE',
          100: '#F5F8FA',
          200: '#E2E8EF',
          300: '#C6D0DC',
          400: '#94A3B8',
          500: '#64748B',
          600: '#475569',
          700: '#334155',
          800: '#1E293B',
          900: '#0F172A',
        },

        primary: {
          color: '{blue.600}',
          inverseColor: '#FFFFFF',
          hoverColor: '{blue.700}',
          activeColor: '{blue.800}',
        },

        highlight: {
          background: '{blue.50}',
          focusBackground: '{blue.100}',
          color: '{blue.700}',
          focusColor: '{blue.800}',
        },

        formField: {
          disabledBackground: '{surface.100}',
          floatLabel: {
            color: '{surface.500}',
          },
        },
      },
    },
  },

  components: {
    tooltip: {
      root: {
        background: '{surface.800}',
      },
    },
    card: {
      root: {
        shadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
      },
      body: {
        gap: '40px',
      },
    },
    dialog: {
      header: {
        padding: '1rem 1rem 0.5rem 1.5rem',
      },
    },
    progressspinner: {
      root: {
        colorOne: '{blue.600}',
        colorTwo: '{blue.600}',
        colorThree: '{blue.600}',
        colorFour: '{blue.600}',
      },
    },
    scrollpanel: {
      bar: {
        background: '{surface.300}'
      }
    }
  },
});
