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
      css: `
        .p-dialog-footer {
          justify-content: center;
        }
      `,
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
        background: '{surface.300}',
      },
    },
    toolbar: {
      root: {
        background: '{surface.0}',
        borderColor: '{surface.300}',
        borderRadius: '6px 6px 0 0',
      },
      css: `
        .p-toolbar {
          border-bottom: 0;
          box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1);
        }
        .p-toolbar-start, .p-toolbar-center, .p-toolbar-end {
          gap: 5px;
        }
      `,
    },
    datatable: {
      row: {
        selectedBackground: '{primary.100}',
      },
      colorScheme: {
        light: {
          bodyCell: {
            selectedBorderColor: '{content.border.color}',
          },
        },
        dark: {
          bodyCell: {
            selectedBorderColor: '{content.border.color}',
          },
        },
      },
    },
    button: {
      root: {
        secondary: {
          background: '{surface.200}',
          borderColor: '{surface.200}',
          hoverBackground: '{surface.300}',
          hoverBorderColor: '{surface.300}',
        },
        success: {
          activeBackground: '{green.600}',
          activeBorderColor: '{green.600}',
        },
        sm: {
          fontSize: '1rem',
        },
      },
      css: `
        .p-button-sm.p-button-icon-only {
          height: var(--p-button-sm-icon-only-width);;
        }
      `,
    },
    checkbox: {
      css: `
        .p-checkbox {
            border-radius: 50%;
            transition: box-shadow dt('checkbox.transition.duration');
        }
        
        .p-checkbox-box {
            border-width: 2px;
        }
        
        .p-checkbox:not(.p-disabled):has(.p-checkbox-input:hover) {
            box-shadow: 0 0 1px 10px color-mix(in srgb, dt('text.color'), transparent 96%);
        }
        
        .p-checkbox:not(.p-disabled):has(.p-checkbox-input:focus-visible) {
            box-shadow: 0 0 1px 10px color-mix(in srgb, dt('text.color'), transparent 88%);
        }
        
        .p-checkbox-checked:not(.p-disabled):has(.p-checkbox-input:hover) {
            box-shadow: 0 0 1px 10px color-mix(in srgb, dt('checkbox.checked.background'), transparent 92%);
        }
        
        .p-checkbox-checked:not(.p-disabled):has(.p-checkbox-input:focus-visible) {
            box-shadow: 0 0 1px 10px color-mix(in srgb, dt('checkbox.checked.background'), transparent 84%);
        }
        
        .p-checkbox-checked .p-checkbox-box:before  {
            content: "";
            position: absolute;
            top: var(--p-md-check-icon-t);
            left: 2px;
            border-right: 2px solid transparent;
            border-bottom: 2px solid transparent;
            transform: rotate(45deg);
            transform-origin: 0% 100%;
            animation: p-md-check  var(--p-checkbox-check-duration, 125ms) var(--p-checkbox-check-delay, 50ms) linear forwards;
        }
        
        .p-checkbox-checked .p-checkbox-icon {
            display: none;
        }
        
        .p-checkbox {
            --p-md-check-icon-t: 10px;
            --p-md-check-icon-w: 6px;
            --p-md-check-icon-h: 12px;
        }
        
        .p-checkbox-sm {
            --p-md-check-icon-t: 8px;
            --p-md-check-icon-w: 4px;
            --p-md-check-icon-h: 10px;
        }
        
        .p-checkbox-lg {
            --p-md-check-icon-t: 12px;
            --p-md-check-icon-w: 8px;
            --p-md-check-icon-h: 16px;
        }
        
        @keyframes p-md-check {
            0%{
              width: 0;
              height: 0;
              border-color: dt('checkbox.icon.checked.color');
              transform: translate3d(0,0,0) rotate(45deg);
            }
            33%{
              width: var(--p-md-check-icon-w);
              height: 0;
              transform: translate3d(0,0,0) rotate(45deg);
            }
            100%{
              width: var(--p-md-check-icon-w);
              height: var(--p-md-check-icon-h);
              border-color: dt('checkbox.icon.checked.color');
              transform: translate3d(0,calc(-1 * var(--p-md-check-icon-h)),0) rotate(45deg);
            }
        }
      `,
    },
  },
});
