import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import Layout from './components/common/Layout';
import Dashboard from './pages/Dashboard';
import PatternConfigPage from './pages/PatternConfigPage';
import UploadPage from './pages/UploadPage';
import QuestionListPage from './pages/QuestionListPage';
import QuestionEditPage from './pages/QuestionEditPage';
import ExamPage from './pages/ExamPage';

import './App.css';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="patterns" element={<PatternConfigPage />} />
          <Route path="upload" element={<UploadPage />} />
          <Route path="questions" element={<QuestionListPage />} />
          <Route path="questions/:id/edit" element={<QuestionEditPage />} />
          <Route path="exams" element={<ExamPage />} />
        </Route>
      </Routes>
      <ToastContainer
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
      />
    </BrowserRouter>
  );
}

export default App;
