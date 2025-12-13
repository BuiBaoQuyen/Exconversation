import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { uploadAPI } from '../services/api';
import FileUploader from '../components/Upload/FileUploader';
import UploadList from '../components/Upload/UploadList';
import './UploadPage.css';

function UploadPage() {
  const [uploads, setUploads] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUploads();
  }, []);

  const loadUploads = async () => {
    try {
      setLoading(true);
      const response = await uploadAPI.getAll();
      setUploads(response.data || []);
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Unknown error';
      console.error('Failed to load uploads:', error);
      toast.error('Failed to load uploads: ' + errorMessage);
      setUploads([]); // Set empty array on error to prevent crash
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (file, uploadedByName) => {
    try {
      const response = await uploadAPI.upload(file, uploadedByName);
      toast.success('File uploaded successfully. AI parsing started automatically.');
      // Reload uploads after a short delay to ensure backend processed
      setTimeout(() => {
        loadUploads();
      }, 500);
      return response.data;
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Unknown error';
      console.error('Upload error:', error);
      toast.error('Failed to upload file: ' + errorMessage);
      // Don't throw error to prevent component crash
      return null;
    }
  };

  const handleParse = async (upload) => {
    try {
      await uploadAPI.parse(upload.id);
      toast.success('Parsing started. Please check status shortly.');
      // Reload uploads after a short delay
      setTimeout(() => {
        loadUploads();
      }, 500);
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Unknown error';
      console.error('Parse error:', error);
      toast.error('Failed to start parsing: ' + errorMessage);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this upload?')) {
      return;
    }
    try {
      await uploadAPI.delete(id);
      toast.success('Upload deleted successfully');
      loadUploads();
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || error.message || 'Unknown error';
      console.error('Delete error:', error);
      toast.error('Failed to delete upload: ' + errorMessage);
    }
  };

  // Always render, even if loading or error
  return (
    <div className="upload-page">
      <div className="page-header">
        <h1>Upload & Parse</h1>
        <p className="page-description">
          Upload DOCX files. AI will automatically detect and parse questions, answers, and chapters.
        </p>
      </div>

      {loading ? (
        <div className="loading">Loading uploads...</div>
      ) : (
        <div>
          <FileUploader onUpload={handleUpload} />
          <UploadList
            uploads={uploads || []}
            onParse={handleParse}
            onDelete={handleDelete}
            onRefresh={loadUploads}
          />
        </div>
      )}
    </div>
  );
}

export default UploadPage;

