import { createBrowserRouter, Navigate } from 'react-router-dom'
import { TopNavLayout } from '@/shared/ui'
import { ProjectShell } from '@/shared/ui'
import { HealthPage } from '@/pages/health/HealthPage'
import { ProjectsPage } from '@/pages/projects/ProjectsPage'
import { ProjectConfigSection } from '@/pages/project-detail/ProjectConfigSection'
import { ProjectDataSection } from '@/pages/project-detail/ProjectDataSection'
import { ProjectPreprocessingSection } from '@/pages/project-detail/ProjectPreprocessingSection'
import { ProjectScenariosSection } from '@/pages/project-detail/ProjectScenariosSection'
import { ProjectResultsSection } from '@/pages/project-results/ProjectResultsSection'
import { DatasetPage } from '@/pages/dataset/DatasetPage'
import { ScenarioCreatePage } from '@/pages/scenario-create/ScenarioCreatePage'
import { ScenarioEditPage } from '@/pages/scenario-edit/ScenarioEditPage'
import { SimulationTestPage } from '@/pages/simulation-test/SimulationTestPage'
import { RunMonitorPage } from '@/pages/run-monitor/RunMonitorPage'
import { CatalogPage } from '@/pages/catalog/CatalogPage'
import { ParamCatalogPage } from '@/pages/param-catalog/ParamCatalogPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/projects" replace />,
  },

  // --- Page liste des projets : pas de sidebar ---
  {
    path: '/projects',
    element: (
      <TopNavLayout>
        <ProjectsPage />
      </TopNavLayout>
    ),
  },

  // --- Catalogue de données (gestion globale des fichiers/champs) ---
  {
    path: '/catalog',
    element: (
      <TopNavLayout>
        <CatalogPage />
      </TopNavLayout>
    ),
  },

  // --- Catalogue des paramètres de scénario (gestion globale) ---
  {
    path: '/scenario-parameters',
    element: (
      <TopNavLayout>
        <ParamCatalogPage />
      </TopNavLayout>
    ),
  },

  // --- Simulation de test (validation de la communication, sans projet) ---
  {
    path: '/test',
    element: (
      <TopNavLayout>
        <SimulationTestPage />
      </TopNavLayout>
    ),
  },

  // --- Espace projet : sidebar de navigation ---
  {
    path: '/projects/:id',
    element: <ProjectShell />,
    children: [
      { index: true, element: <Navigate to="config" replace /> },
      { path: 'config',              element: <ProjectConfigSection /> },
      { path: 'data',                element: <ProjectDataSection /> },
      { path: 'data/:datasetId',     element: <DatasetPage /> },
      { path: 'preprocessing',       element: <ProjectPreprocessingSection /> },
      { path: 'scenarios',                 element: <ProjectScenariosSection /> },
      { path: 'scenarios/new',             element: <ScenarioCreatePage /> },
      { path: 'scenarios/:scenarioId/edit', element: <ScenarioEditPage /> },
      { path: 'results',             element: <ProjectResultsSection /> },
      { path: 'runs/:runId',         element: <RunMonitorPage /> },
    ],
  },

  // --- Monitoring de run (top bar uniquement) ---
  {
    path: '/runs/:id',
    element: (
      <TopNavLayout>
        <RunMonitorPage />
      </TopNavLayout>
    ),
  },

  {
    path: '/_health',
    element: <HealthPage />,
  },
])
