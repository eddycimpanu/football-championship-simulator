export default function ServerDown({ title = 'The server is not answering' }) {
  return (
    <div className="notice" role="alert">
      <h1 className="page-title">{title}</h1>
      <p>Check that WebApplication is running in IntelliJ, then reload this page.</p>
    </div>
  );
}
